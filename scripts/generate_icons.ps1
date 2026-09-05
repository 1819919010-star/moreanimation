Add-Type -AssemblyName System.Drawing

function Write-PixelIcon([string]$Path, [string[]]$Rows, [hashtable]$Palette) {
    if ($Rows.Count -ne 16) { throw "Icon must have exactly 16 rows" }
    $bitmap = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt 16; $y++) {
            if ($Rows[$y].Length -ne 16) { throw "Row $y is not 16 pixels: $($Rows[$y])" }
            for ($x = 0; $x -lt 16; $x++) {
                $key = [string]$Rows[$y][$x]
                $color = if ($key -eq '.') { [System.Drawing.Color]::Transparent } else { $Palette[$key] }
                $bitmap.SetPixel($x, $y, $color)
            }
        }
        $parent = Split-Path -Parent $Path
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
        $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

$itemDir = Join-Path $PSScriptRoot '..\src\main\resources\assets\moreanimation\textures\item'
# hand.png is generated artwork and intentionally is not overwritten by this
# deterministic helper; this script now only rebuilds the terminal icon.

$terminalPalette = @{
    D = [System.Drawing.Color]::FromArgb(255, 28, 33, 43)
    M = [System.Drawing.Color]::FromArgb(255, 61, 73, 91)
    L = [System.Drawing.Color]::FromArgb(255, 111, 129, 153)
    C = [System.Drawing.Color]::FromArgb(255, 38, 220, 226)
    G = [System.Drawing.Color]::FromArgb(255, 91, 218, 74)
    H = [System.Drawing.Color]::FromArgb(255, 174, 246, 84)
    R = [System.Drawing.Color]::FromArgb(255, 206, 67, 60)
}
$terminalRows = @(
    '................',
    '....DDDDDDDD....',
    '...DMLLLLLMD....',
    '..DMMDDDDMMMD...',
    '..DMDGGDDDCMD...',
    '..DMDGHGDDCMD...',
    '..DMDGHHGDDMD...',
    '..DMDGHGDDDMD...',
    '..DMDGGDDDDMD...',
    '..DMMDDDDMMMD...',
    '..DMLLLLLLLMD...',
    '..DMDMDDMRDMD...',
    '..DMLMLLLMRMD...',
    '...DMMMMMMMD....',
    '....DDDDDDD.....',
    '................'
)
Write-PixelIcon (Join-Path $itemDir 'animation_control_terminal.png') $terminalRows $terminalPalette
