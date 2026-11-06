"""Genera los recursos Android a partir del logo oficial, sin deformarlo."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
source = Image.open(ROOT / "logo.png").convert("RGBA")
logo = source.crop(source.getbbox())
BACKGROUND = "#F4F6FA"


def centered(size, width, background=(0, 0, 0, 0)):
    artwork = logo.resize((width, round(width * logo.height / logo.width)), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), background)
    canvas.alpha_composite(artwork, ((size - artwork.width) // 2, (size - artwork.height) // 2))
    return canvas


for name, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    folder = RES / f"mipmap-{name}"
    folder.mkdir(exist_ok=True)
    icon = centered(size, round(size * 0.70), BACKGROUND)
    icon.save(folder / "ic_launcher.webp", lossless=True)
    mask = Image.new("L", (size, size))
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    icon.putalpha(mask)
    icon.save(folder / "ic_launcher_round.webp", lossless=True)

folder = RES / "drawable-xxxhdpi"
folder.mkdir(exist_ok=True)
foreground = centered(432, 240)
foreground.save(folder / "ic_launcher_foreground.png", optimize=True)
monochrome = Image.new("RGBA", foreground.size, "white")
monochrome.putalpha(ImageChops.multiply(foreground.getchannel("R"), foreground.getchannel("A")))
monochrome.save(folder / "ic_launcher_monochrome.png", optimize=True)
centered(1152, 720).save(folder / "ic_splash_logo.png", optimize=True)
