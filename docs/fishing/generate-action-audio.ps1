# Оригинальные короткие звуковые мотивы: PCM16, mono, 22050 Hz. Фоновой музыки нет.
$audioDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../feature/mini-games/fishing/src/main/assets/fishing/audio'))
[IO.Directory]::CreateDirectory($audioDirectory) | Out-Null
$sampleRate = 22050
$cues = @{
    charge = @{ Notes = @(392, 523.25); NoteSeconds = .09; Kind = 'pluck' }
    cast = @{ Notes = @(740, 330); NoteSeconds = .12; Kind = 'sweep' }
    splash = @{ Notes = @(220, 440, 330); NoteSeconds = .055; Kind = 'water' }
    bite = @{ Notes = @(659.25, 987.77); NoteSeconds = .09; Kind = 'bell' }
    reel = @{ Notes = @(293.66); NoteSeconds = .045; Kind = 'pluck' }
    relax = @{ Notes = @(523.25, 392); NoteSeconds = .075; Kind = 'pluck' }
    warning = @{ Notes = @(440, 440); NoteSeconds = .09; Kind = 'bell' }
    catch = @{ Notes = @(523.25, 659.25, 783.99, 1046.5); NoteSeconds = .085; Kind = 'bell' }
    release = @{ Notes = @(783.99, 659.25, 523.25); NoteSeconds = .075; Kind = 'water' }
    escape = @{ Notes = @(392, 329.63, 261.63); NoteSeconds = .075; Kind = 'pluck' }
    junk = @{ Notes = @(180, 130); NoteSeconds = .055; Kind = 'pluck' }
    hazard = @{ Notes = @(330, 247); NoteSeconds = .10; Kind = 'bell' }
    pop = @{ Notes = @(110, 80, 60); NoteSeconds = .10; Kind = 'water' }
}
foreach ($name in ($cues.Keys | Sort-Object)) {
    $cue = $cues[$name]
    $samplesPerNote = [int]($sampleRate * $cue.NoteSeconds)
    $count = $samplesPerNote * $cue.Notes.Count
    $writer = [IO.BinaryWriter]::new([IO.File]::Create((Join-Path $audioDirectory "$name.wav")))
    try {
        $writer.Write([Text.Encoding]::ASCII.GetBytes('RIFF'))
        $writer.Write([int](36 + $count * 2))
        $writer.Write([Text.Encoding]::ASCII.GetBytes('WAVEfmt '))
        $writer.Write([int]16); $writer.Write([short]1); $writer.Write([short]1)
        $writer.Write([int]$sampleRate); $writer.Write([int]($sampleRate * 2))
        $writer.Write([short]2); $writer.Write([short]16)
        $writer.Write([Text.Encoding]::ASCII.GetBytes('data')); $writer.Write([int]($count * 2))
        for ($n = 0; $n -lt $cue.Notes.Count; $n++) {
            $frequency = $cue.Notes[$n]
            for ($i = 0; $i -lt $samplesPerNote; $i++) {
                $t = $i / $sampleRate
                $p = $i / $samplesPerNote
                $envelope = [Math]::Min(1, $t / .006) * [Math]::Pow(1 - $p, 1.8)
                $phase = 2 * [Math]::PI * $frequency * $t
                $sample = switch ($cue.Kind) {
                    'bell' { .7 * [Math]::Sin($phase) + .18 * [Math]::Sin($phase * 2) + .08 * [Math]::Sin($phase * 3) }
                    'water' { .6 * [Math]::Sin($phase * (1 + $p * .5)) + .15 * [Math]::Sin($phase * 3.17) }
                    'sweep' { .7 * [Math]::Sin($phase * (1 - $p * .35)) }
                    default { .7 * [Math]::Sin($phase) + .14 * [Math]::Sin($phase * 2) }
                }
                $writer.Write([short]([Math]::Round($sample * $envelope * 16000)))
            }
        }
    } finally { $writer.Dispose() }
}
Write-Output "Generated $($cues.Count) action cues in $audioDirectory"
