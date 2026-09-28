const fs = require('node:fs/promises');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const crypto = require('node:crypto');
const sharp = require('sharp');

// Generate candidates separately from the checkout. No resizing or cropping.
const sha256 = bytes => crypto.createHash('sha256').update(bytes).digest('hex');

function compareRgba(original, decoded) {
    if (original.info.width !== decoded.info.width || original.info.height !== decoded.info.height) {
        throw new Error('Image dimensions changed');
    }
    let squaredError = 0;
    let count = 0;
    let maxRgbDelta = 0;
    const histogram = Array(256).fill(0);
    for (let index = 0; index < original.data.length; index += 4) {
        if (original.data[index + 3] !== decoded.data[index + 3]) throw new Error('Alpha channel changed');
        if (original.data[index + 3] === 0) continue;
        for (let channel = 0; channel < 3; channel++) {
            const delta = Math.abs(original.data[index + channel] - decoded.data[index + channel]);
            squaredError += delta * delta;
            maxRgbDelta = Math.max(maxRgbDelta, delta);
            histogram[delta]++;
            count++;
        }
    }
    let cumulative = 0;
    let p99RgbDelta = 0;
    for (let delta = 0; delta < histogram.length; delta++) {
        cumulative += histogram[delta];
        if (cumulative >= count * 0.99) { p99RgbDelta = delta; break; }
    }
    return {
        rgbaIdentical: original.data.equals(decoded.data),
        alphaIdentical: true,
        maxRgbDelta,
        p99RgbDelta,
        psnr: squaredError === 0 ? 100 : 10 * Math.log10(255 * 255 / (squaredError / count)),
    };
}

async function compress(source, output, relativePath) {
    const input = path.join(source, relativePath);
    const originalBytes = await fs.readFile(input);
    const metadata = await sharp(originalBytes).metadata();
    if (metadata.icc || (metadata.pages ?? 1) > 1) return null;
    const original = await sharp(originalBytes).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
    const isSurface = path.basename(relativePath).startsWith('surface_');
    const isPng = relativePath.endsWith('.png');
    const isResource = relativePath.includes('/src/main/res/drawable-nodpi/');
    let chosen;

    const tryWebp = async (options, encoding, acceptable) => {
        const bytes = await sharp(originalBytes).webp({ effort: 6, alphaQuality: 100, exact: true, ...options }).toBuffer();
        if (bytes.length >= originalBytes.length * 0.98) return false;
        const decoded = await sharp(bytes).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
        const metrics = compareRgba(original, decoded);
        if (!acceptable(metrics)) return false;
        chosen = { bytes, encoding, metrics };
        return true;
    };

    if (isSurface) {
        for (const quality of [95, 98, 100]) {
            if (await tryWebp({ quality, smartSubsample: true }, `webp-q${quality}`,
                metrics => metrics.psnr >= 40 && metrics.p99RgbDelta <= 12)) break;
        }
        if (!chosen) await tryWebp({ nearLossless: true, quality: 90 }, 'webp-near-lossless90',
            metrics => metrics.maxRgbDelta <= 1);
    } else if (isPng && isResource) {
        await tryWebp({ lossless: true, quality: 100 }, 'webp-lossless', metrics => metrics.rgbaIdentical);
    } else if (isPng) {
        const bytes = await sharp(originalBytes).png({ compressionLevel: 9, adaptiveFiltering: true }).toBuffer();
        if (bytes.length < originalBytes.length * 0.98) {
            const decoded = await sharp(bytes).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
            const metrics = compareRgba(original, decoded);
            if (metrics.rgbaIdentical) chosen = { bytes, encoding: 'png-lossless', metrics };
        }
    } else {
        await tryWebp({ nearLossless: true, quality: 90 }, 'webp-near-lossless90',
            metrics => metrics.maxRgbDelta <= 1);
    }
    if (!chosen) return null;

    const outputPath = isPng && isResource ? relativePath.replace(/\.png$/, '.webp') : relativePath;
    if (outputPath !== relativePath && await fs.stat(path.join(source, outputPath)).catch(() => null)) {
        throw new Error(`Resource name collision: ${outputPath}`);
    }
    const destination = path.join(output, outputPath);
    await fs.mkdir(path.dirname(destination), { recursive: true });
    await fs.writeFile(destination, chosen.bytes);
    return {
        path: relativePath, outputPath, width: metadata.width, height: metadata.height,
        sourceBytes: originalBytes.length, outputBytes: chosen.bytes.length,
        sourceSha256: sha256(originalBytes), outputSha256: sha256(chosen.bytes),
        encoding: chosen.encoding, ...chosen.metrics,
    };
}

async function main() {
    const [sourceArg, outputArg] = process.argv.slice(2);
    if (!sourceArg || !outputArg) throw new Error('Usage: node compress_packaged_images.cjs <checkout> <separate-output-directory>');
    const source = path.resolve(sourceArg);
    const output = path.resolve(outputArg);
    const sourceKey = process.platform === 'win32' ? source.toLowerCase() : source;
    const outputKey = process.platform === 'win32' ? output.toLowerCase() : output;
    if (sourceKey === outputKey || outputKey.startsWith(sourceKey + path.sep)) throw new Error('Use an output directory outside the checkout');
    await fs.mkdir(output, { recursive: true });
    const files = execFileSync('git', ['ls-files'], { cwd: source, encoding: 'utf8' }).trim().split(/\r?\n/);
    const selected = [];
    for (const file of files) {
        if (!file.includes('/src/main/') || !/\.(png|webp)$/.test(file) || file.endsWith('.9.png')) continue;
        const resource = file.includes('/src/main/res/drawable-nodpi/');
        const asset = file.includes('/src/main/assets/');
        if (!resource && !asset) continue;
        const bytes = await fs.readFile(path.join(source, file));
        if (bytes.length < 100_000) continue;
        if (file.endsWith('.webp') && !bytes.subarray(0, 40).includes(Buffer.from('VP8L'))) continue;
        // Layered pet art stays byte-identical; its geometry also drives hit testing.
        if (file.startsWith('feature/pet/')) continue;
        selected.push(file);
    }
    let next = 0;
    let completed = 0;
    const images = [];
    sharp.concurrency(1);
    await Promise.all(Array.from({ length: 3 }, async () => {
        while (next < selected.length) {
            const relativePath = selected[next++];
            const result = await compress(source, output, relativePath);
            if (result) images.push(result);
            completed++;
            if (completed % 20 === 0) console.log(JSON.stringify({ completed, selected: selected.length, compressed: images.length }));
        }
    }));
    images.sort((a,b) => a.path.localeCompare(b.path));
    const total = {
        selected: selected.length, compressed: images.length,
        sourceBytes: images.reduce((sum, image) => sum + image.sourceBytes, 0),
        outputBytes: images.reduce((sum, image) => sum + image.outputBytes, 0),
    };
    await fs.writeFile(path.join(output, 'compression-report.json'), JSON.stringify({ total, images }, null, 2));
    console.log(JSON.stringify(total));
}

main().catch(error => { console.error(error); process.exitCode = 1; });
