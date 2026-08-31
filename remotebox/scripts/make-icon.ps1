param(
    [string]$OutDir = "C:\Users\Youssef\Desktop\001-taoukit\remotebox"
)
Add-Type -AssemblyName System.Drawing

$resDir  = Join-Path $OutDir "src\main\resources\icons"
$iconDir = Join-Path $OutDir "icon"
New-Item -ItemType Directory -Force -Path $resDir  | Out-Null
New-Item -ItemType Directory -Force -Path $iconDir | Out-Null

# ---------------------------------------------------------------------------
# Draw the RemoteBox mark on a 256-unit canvas: a screen with a play triangle
# (screen mirroring) sitting on a stand, plus broadcast arcs = remote link.
# ---------------------------------------------------------------------------
function Draw-Mark {
    param([System.Drawing.Graphics]$g, [int]$s)

    $g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.ScaleTransform($s / 256.0, $s / 256.0)

    # rounded-square background with a diagonal gradient
    $rect = New-Object System.Drawing.RectangleF 8, 8, 240, 240
    $bg = New-Object System.Drawing.Drawing2D.GraphicsPath
    $r = 58
    $bg.AddArc($rect.X, $rect.Y, $r, $r, 180, 90)
    $bg.AddArc($rect.Right - $r, $rect.Y, $r, $r, 270, 90)
    $bg.AddArc($rect.Right - $r, $rect.Bottom - $r, $r, $r, 0, 90)
    $bg.AddArc($rect.X, $rect.Bottom - $r, $r, $r, 90, 90)
    $bg.CloseFigure()
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.Color]::FromArgb(255, 37, 99, 235),
        [System.Drawing.Color]::FromArgb(255, 6, 182, 212), 55)
    $g.FillPath($grad, $bg)

    $white = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)

    # broadcast arcs (top-left) -> "remote"
    $penArc = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(235, 255, 255, 255)), 11
    $penArc.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $penArc.EndCap   = [System.Drawing.Drawing2D.LineCap]::Round
    $cx = 74; $cy = 74
    foreach ($rad in 20, 40, 60) {
        $g.DrawArc($penArc, $cx - $rad, $cy - $rad, $rad * 2, $rad * 2, 200, 70)
    }
    $g.FillEllipse($white, $cx - 8, $cy - 8, 16, 16)

    # screen panel
    $screen = New-Object System.Drawing.Drawing2D.GraphicsPath
    $sx = 62; $sy = 84; $sw = 150; $sh = 104; $sr = 18
    $screen.AddArc($sx, $sy, $sr, $sr, 180, 90)
    $screen.AddArc($sx + $sw - $sr, $sy, $sr, $sr, 270, 90)
    $screen.AddArc($sx + $sw - $sr, $sy + $sh - $sr, $sr, $sr, 0, 90)
    $screen.AddArc($sx, $sy + $sh - $sr, $sr, $sr, 90, 90)
    $screen.CloseFigure()
    $g.FillPath($white, $screen)
    $g.FillPath((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 30, 41, 59))),
        (Inset-Path $sx $sy $sw $sh $sr 10))

    # play triangle inside the screen
    $tri = @(
        (New-Object System.Drawing.PointF(($sx + 52), ($sy + 26))),
        (New-Object System.Drawing.PointF(($sx + 108), ($sy + 52))),
        (New-Object System.Drawing.PointF(($sx + 52), ($sy + 78)))
    )
    $g.FillPolygon($white, $tri)

    # stand
    $g.FillRectangle($white, ($sx + $sw / 2 - 12), ($sy + $sh), 24, 20)
    $stand = New-Object System.Drawing.Drawing2D.GraphicsPath
    $stand.AddArc(($sx + $sw / 2 - 42), ($sy + $sh + 20), 12, 12, 90, 90)
    $stand.AddArc(($sx + $sw / 2 + 30), ($sy + $sh + 20), 12, 12, 0, -90)
    $g.FillPath($white, $stand)
    $g.FillRectangle($white, ($sx + $sw / 2 - 42), ($sy + $sh + 20), 84, 12)
}

function Inset-Path {
    param($x, $y, $w, $h, $rr, $pad)
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $x2 = $x + $pad; $y2 = $y + $pad; $w2 = $w - 2 * $pad; $h2 = $h - 2 * $pad; $r2 = [Math]::Max(4, $rr - $pad)
    $p.AddArc($x2, $y2, $r2, $r2, 180, 90)
    $p.AddArc($x2 + $w2 - $r2, $y2, $r2, $r2, 270, 90)
    $p.AddArc($x2 + $w2 - $r2, $y2 + $h2 - $r2, $r2, $r2, 0, 90)
    $p.AddArc($x2, $y2 + $h2 - $r2, $r2, $r2, 90, 90)
    $p.CloseFigure()
    return $p
}

function Render-Png {
    param([int]$size)
    $bmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    Draw-Mark $g $size
    $g.Dispose()
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    return , $ms.ToArray()
}

$sizes = 16, 24, 32, 48, 64, 128, 256
$pngs  = @{}
foreach ($sz in $sizes) {
    $bytes = Render-Png $sz
    $pngs[$sz] = $bytes
    [System.IO.File]::WriteAllBytes((Join-Path $resDir "remotebox-$sz.png"), $bytes)
}
Write-Host "PNGs -> $resDir  ($($sizes -join ', '))"

# ---- assemble a multi-resolution .ico (PNG-compressed entries) ----
$icoPath = Join-Path $iconDir "remotebox.ico"
$fs = [System.IO.File]::Create($icoPath)
$bw = New-Object System.IO.BinaryWriter($fs)
$bw.Write([UInt16]0); $bw.Write([UInt16]1); $bw.Write([UInt16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
foreach ($sz in $sizes) {
    $len = $pngs[$sz].Length
    $dim = if ($sz -ge 256) { 0 } else { $sz }
    $bw.Write([Byte]$dim); $bw.Write([Byte]$dim)
    $bw.Write([Byte]0); $bw.Write([Byte]0)
    $bw.Write([UInt16]1); $bw.Write([UInt16]32)
    $bw.Write([UInt32]$len); $bw.Write([UInt32]$offset)
    $offset += $len
}
foreach ($sz in $sizes) { $bw.Write($pngs[$sz]) }
$bw.Flush(); $bw.Close()
Write-Host "ICO  -> $icoPath  ($([Math]::Round((Get-Item $icoPath).Length / 1kb, 1)) KB, $($sizes.Count) tailles)"
