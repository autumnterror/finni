const fs = require('node:fs/promises');
const path = require('node:path');
const sharp = require('sharp');

// Lossless only: keep every decoded RGBA byte, including transparent pixels.
async function main() {
    const [sourceArg, outputArg] = process.argv.slice(2);
    if (!sourceArg || !outputArg) throw new Error('Usage: node optimize_room_assets.cjs <source-directory> <output-directory>');
    const source = path.resolve(sourceArg);
    const output = path.resolve(outputArg);
    if (source === output) throw new Error('Use a separate output directory; retain the source artwork.');
    await fs.mkdir(output, { recursive: true });
    const names = (await fs.readdir(source)).filter(name => /^room_(object|decor)_[a-z_]+\.(png|webp)$/.test(name)).sort();
    if (!names.length) throw new Error('No room sprites found.');
    if (new Set(names.map(name => path.parse(name).name)).size !== names.length) {
        throw new Error('Keep only one source file per sprite.');
    }
    const report = [];

    for (const name of names) {
        const input = path.join(source, name);
        const originalBytes = await fs.readFile(input);
        const original = await sharp(input).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
        let encoded = originalBytes;
        let outputName = name;
        let encoding = 'original';
        const presets = originalBytes.length >= 250_000 ? ['default', 'drawing', 'icon'] : ['default'];
        for (const preset of presets) {
            const candidate = await sharp(input).webp({
                lossless: true, quality: 100, effort: 6, preset,
            }).toBuffer();
            if (candidate.length >= encoded.length) continue;
            const decoded = await sharp(candidate).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
            if (original.info.width !== decoded.info.width || original.info.height !== decoded.info.height ||
                !original.data.equals(decoded.data)) continue;
            encoded = candidate;
            outputName = name.replace(/\.png$/, '.webp');
            encoding = `lossless-${preset}`;
        }
        const destination = path.join(output, outputName);
        if (await fs.stat(destination).catch(() => null)) throw new Error(`Output already exists: ${destination}`);
        await fs.writeFile(destination, encoded);
        const item = {
            name, outputName, width: original.info.width, height: original.info.height,
            sourceBytes: originalBytes.length, outputBytes: encoded.length,
            rgbaIdentical: true, encoding,
        };
        report.push(item);
        console.log(JSON.stringify(item));
    }
    const total = {
        sourceBytes: report.reduce((sum, item) => sum + item.sourceBytes, 0),
        outputBytes: report.reduce((sum, item) => sum + item.outputBytes, 0),
        count: report.length,
    };
    await fs.writeFile(path.join(output, 'compression-report.json'), JSON.stringify({ total, sprites: report }, null, 2));
    console.log(JSON.stringify(total));
}

main().catch(error => { console.error(error); process.exitCode = 1; });
