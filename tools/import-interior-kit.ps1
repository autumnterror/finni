param(
    [Parameter(Mandatory = $true)][string]$ArchivePath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$assetRoot = Join-Path $root 'feature/room/src/main/res/drawable-nodpi'
$catalogPath = Join-Path $root 'feature/room/src/main/assets/interior_catalog.json'
$slotToPlacement = [ordered]@{
    room_bed = 'bed'; room_wardrobe = 'wardrobe'; room_bedside_table = 'decor_bedside_table'
    room_lamp = 'decor_lamp'; room_standing_mirror = 'decor_mirror'; room_window = 'decor_window'
    room_rug_bedroom = 'decor_rug_bedroom'
    room_sofa = 'decor_sofa'; room_coffee_table = 'decor_coffee_table'
    room_green_cabinet = 'decor_cabinet'; room_piggy_bank = 'piggy_bank'
    room_rug_living = 'decor_rug_living'; room_phone_station = 'phone'
    room_shelf_phone = 'decor_shelf_phone'; room_plant = 'decor_plant'
    room_notice_board = 'decor_notice_board'; room_calendar = 'calendar'
    room_open_book = 'task_board'; room_pencil = 'decor_pencil'
    room_fridge = 'fridge'; room_stove = 'decor_stove'; room_range_hood = 'decor_range_hood'
    room_kitchen_cabinet = 'sink'; room_dining_table = 'dining_table'
    room_chair = 'decor_chair'; room_rug_kitchen = 'decor_rug_kitchen'
    room_cutting_board = 'decor_cutting_board'
}
$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ArchivePath).Path)
try {
    $manifestStream = [IO.StreamReader]::new($archive.GetEntry('Finni_Interior_Kit/manifest.json').Open())
    try { $manifest = $manifestStream.ReadToEnd() | ConvertFrom-Json } finally { $manifestStream.Dispose() }
    $chipStream = [IO.StreamReader]::new($archive.GetEntry('Finni_Interior_Kit/integration/room_object_chips.json').Open())
    try { $chips = $chipStream.ReadToEnd() | ConvertFrom-Json } finally { $chipStream.Dispose() }
    New-Item -ItemType Directory -Force -Path $assetRoot, (Split-Path $catalogPath) | Out-Null
    $rooms = @('bedroom', 'living', 'kitchen')
    $slotRecords = foreach ($room in $rooms) {
        foreach ($chip in $chips.rooms.$room) {
            if (!$slotToPlacement.Contains($chip.slot_id)) { continue }
            [ordered]@{ id = $chip.slot_id; placement = $slotToPlacement[$chip.slot_id]; room = $room; label = $chip.label }
        }
    }
    $originalsById = @{}
    foreach ($original in $manifest.originals) { $originalsById[$original.id] = $original }
    $supportedParents = @('room_bedside_table', 'room_green_cabinet', 'room_coffee_table', 'room_shelf_phone', 'room_kitchen_cabinet')
    $variants = foreach ($item in $manifest.assets) {
        if (!$slotToPlacement.Contains($item.slot_id)) { continue }
        $resourceName = 'interior_' + ($item.id -replace '[^a-z0-9]+', '_').Trim('_')
        $entry = $archive.GetEntry('Finni_Interior_Kit/' + $item.webp)
        if ($null -eq $entry) { throw "Missing WebP: $($item.webp)" }
        $target = Join-Path $assetRoot ($resourceName + '.webp')
        $inputStream = $entry.Open()
        try {
            $outputStream = [IO.File]::Create($target)
            try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose() }
        } finally { $inputStream.Dispose() }
        $supportDelta = 0.0
        if ($item.support_anchor_normalized -and $item.source_support_anchor_normalized) {
            $supportDelta = [double]$item.support_anchor_normalized[1] - [double]$item.source_support_anchor_normalized[1]
        } elseif ($item.slot_id -in $supportedParents) {
            $original = $originalsById[$item.slot_id]
            $supportDelta = ([double]$item.content_bounds_px[1] - [double]$original.content_bounds_px[1]) / [double]$original.canvas_px[1]
        }
        [ordered]@{
            id = $item.id; slot = $item.slot_id; name = $item.name_ru
            price = [long]$item.price_demo; drawable = $resourceName
            supportDelta = [math]::Round($supportDelta, 6)
        }
    }
    $catalog = [ordered]@{ schema = 1; rooms = $rooms; slots = @($slotRecords); variants = @($variants) }
    [IO.File]::WriteAllText($catalogPath, ($catalog | ConvertTo-Json -Depth 8 -Compress), [Text.UTF8Encoding]::new($false))
    Write-Output "Imported $($variants.Count) WebP variants for $($slotRecords.Count) room placements."
} finally { $archive.Dispose() }
