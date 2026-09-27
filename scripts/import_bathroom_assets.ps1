param(
    [Parameter(Mandatory = $true)][string]$ArchivePath
)

Add-Type -AssemblyName System.IO.Compression
$projectRoot = Split-Path -Parent $PSScriptRoot
$drawableDirectory = Join-Path $projectRoot 'feature/room/src/main/res/drawable-nodpi'
$catalogPath = Join-Path $projectRoot 'feature/room/src/main/assets/interior_catalog.json'
$archive = [IO.Compression.ZipFile]::OpenRead($ArchivePath)
try {
    $entry = $archive.GetEntry('Finni_Bathroom_Kit/manifest.json')
    if ($null -eq $entry) { throw 'Bathroom manifest is missing' }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { $manifest = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    $catalog = Get-Content -Raw -LiteralPath $catalogPath | ConvertFrom-Json
    $bathSlotIds = @($manifest.originals | ForEach-Object { $_.slot_id })
    $oldSlots = @($catalog.slots | Where-Object { $_.room -ne 'bathroom' })
    $oldVariants = @($catalog.variants | Where-Object { $_.slot -notin $bathSlotIds })

    function Copy-Webp($sourcePath, $resourceName) {
        $source = $archive.GetEntry('Finni_Bathroom_Kit/' + $sourcePath)
        if ($null -eq $source) { throw "Missing asset $sourcePath" }
        $destination = Join-Path $drawableDirectory ($resourceName + '.webp')
        $inputStream = $source.Open()
        $outputStream = [IO.File]::Create($destination)
        try { $inputStream.CopyTo($outputStream) } finally { $inputStream.Dispose(); $outputStream.Dispose() }
    }

    $slotNames = @{}
    foreach ($asset in $manifest.originals) {
        $slotNames[$asset.slot_id] = $asset.object_name_ru
        Copy-Webp $asset.webp ('bath_' + ($asset.id -replace '__','_'))
        if ($null -ne $asset.front_layer) {
            Copy-Webp $asset.front_layer.webp ('bath_front_' + ($asset.id -replace '__','_'))
        }
    }
    foreach ($asset in $manifest.assets) {
        Copy-Webp $asset.webp ('bath_' + ($asset.id -replace '__','_'))
        if ($null -ne $asset.front_layer) {
            Copy-Webp $asset.front_layer.webp ('bath_front_' + ($asset.id -replace '__','_'))
        }
    }
    $bathSlots = @($manifest.originals | ForEach-Object {
        [ordered]@{ id = $_.slot_id; placement = $_.slot_id; room = 'bathroom'; label = $_.object_name_ru }
    })
    $bathVariants = @($manifest.assets | ForEach-Object {
        [ordered]@{
            id = $_.id
            slot = $_.slot_id
            name = ($slotNames[$_.slot_id] + ' · ' + $_.name_ru)
            price = [int]$_.price_demo
            drawable = ('bath_' + ($_.id -replace '__','_'))
            supportDelta = 0
        }
    })
    $catalog.slots = @($oldSlots) + $bathSlots
    $catalog.variants = @($oldVariants) + $bathVariants
    [IO.File]::WriteAllText($catalogPath, ($catalog | ConvertTo-Json -Depth 10 -Compress), [Text.UTF8Encoding]::new($false))
    Write-Output "Imported $($bathSlots.Count) bathroom slots and $($bathVariants.Count) variants"
} finally {
    $archive.Dispose()
}
