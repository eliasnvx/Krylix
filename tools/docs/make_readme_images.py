#!/usr/bin/env python3
"""README / mod page images for Krylix: banner, section headers and screenshot galleries.

Backgrounds, the pixel font, icons' frames and chips are drawn here; item icons come from the vanilla client jar
of the Minecraft version in buildSrc/Versions.kt; the pictures are real in-game screenshots taken by
DocsShotsClientTest (never AI-generated).

    KRYLIX_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest     # takes the docs_* screenshots
    python3 tools/docs/make_readme_images.py                      # writes docs/images/*.png
    python3 tools/docs/make_readme_images.py --page-base URL      # also writes docs/pages/modrinth.md (absolute URLs)
"""
import argparse
import glob
import io
import math
import os
import re
import zipfile

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
SHOTS = os.path.join(ROOT, "fabric", "build", "run", "clientGameTest", "screenshots")
OUT = os.path.join(ROOT, "docs", "images")

# ------------------------------------------------------------------------------------------------ palette

BG_TOP = (27, 23, 33)        # charcoal with a hint of violet
BG_BOTTOM = (48, 16, 24)     # dried-blood crimson
CRIMSON = (214, 48, 60)
CRIMSON_DARK = (92, 18, 28)
GOLD = (255, 196, 64)
BONE = (236, 228, 214)
INK = (18, 14, 20)
WHITE = (255, 255, 255)


def versions():
    """minecraftVersion / coreVersion from buildSrc, so the chips follow the build."""
    text = open(os.path.join(ROOT, "buildSrc", "src", "main", "kotlin", "Versions.kt"), encoding="utf-8").read()

    def const(name):
        m = re.search(r'const val %s\s*=\s*"([^"]+)"' % name, text)
        return m.group(1) if m else "?"
    return const("minecraftVersion"), const("coreVersion")


MC_VERSION, MOD_VERSION = versions()

# ------------------------------------------------------------------------------------------------ pixel font (5x7)

GLYPHS = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["111", "010", "010", "010", "010", "010", "111"],
    "J": ["00111", "00010", "00010", "00010", "00010", "10010", "01100"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "10101", "01010"],
    "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "0": ["01110", "10011", "10101", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00110", "01000", "10000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
    " ": ["000", "000", "000", "000", "000", "000", "000"],
    ".": ["0", "0", "0", "0", "0", "0", "1"],
    ",": ["00", "00", "00", "00", "00", "01", "10"],
    ":": ["0", "1", "0", "0", "0", "1", "0"],
    "!": ["1", "1", "1", "1", "1", "0", "1"],
    "'": ["1", "1", "0", "0", "0", "0", "0"],
    "-": ["000", "000", "000", "111", "000", "000", "000"],
    "+": ["000", "010", "010", "111", "010", "010", "000"],
    "/": ["00001", "00010", "00010", "00100", "01000", "01000", "10000"],
    "&": ["01100", "10010", "10100", "01000", "10101", "10010", "01101"],
    "%": ["11001", "11010", "00010", "00100", "01000", "01011", "10011"],
    "(": ["01", "10", "10", "10", "10", "10", "01"],
    ")": ["10", "01", "01", "01", "01", "01", "10"],
    "#": ["01010", "11111", "01010", "01010", "01010", "11111", "01010"],
    "*": ["01010", "11011", "11111", "11111", "01110", "00100", "00000"],  # heart
}


def text_width(text, scale):
    return sum((len(GLYPHS.get(ch, GLYPHS[" "])[0]) + 1) * scale for ch in text.upper()) - scale


def draw_text(img, text, x, y, scale, fill, shadow=INK, outline=None, shadow_step=None):
    """Pixel text; `*` draws a heart. Shadow one font pixel (or `shadow_step` px) down-right, optional outline."""
    draw = ImageDraw.Draw(img)

    def blit(ox, oy, color):
        cx = ox
        for ch in text.upper():
            glyph = GLYPHS.get(ch, GLYPHS[" "])
            for gy, row in enumerate(glyph):
                for gx, bit in enumerate(row):
                    if bit == "1":
                        draw.rectangle([cx + gx * scale, oy + gy * scale,
                                        cx + (gx + 1) * scale - 1, oy + (gy + 1) * scale - 1], fill=color)
            cx += (len(glyph[0]) + 1) * scale
    if outline:
        step = max(1, scale // 3)
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            blit(x + dx * step, y + dy * step, outline)
    if shadow:
        step = shadow_step or scale
        blit(x + step, y + step, shadow)
    blit(x, y, fill)


# ------------------------------------------------------------------------------------------------ backgrounds

# A small pixel sword, tiled crossed-sword style (every other one mirrored) as a faint pattern.
SWORD = ["0000000011",
         "0000000111",
         "0000001110",
         "0000011100",
         "1000111000",
         "0101110000",
         "0011100000",
         "0011000000",
         "0100100000",
         "1000000000"]


def gradient(w, h, top=BG_TOP, bottom=BG_BOTTOM):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    return img


def sword_pattern(img, cell=64, scale=3, alpha=0.06, color=WHITE):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    a = int(255 * alpha)
    for row, y in enumerate(range(-cell, img.height + cell, cell)):
        for col, x in enumerate(range(-cell, img.width + cell, cell)):
            ox = x + (cell // 2 if row % 2 else 0)
            mirror = (row + col) % 2 == 1
            for gy, line in enumerate(SWORD):
                for gx, bit in enumerate(line):
                    if bit == "1":
                        px = len(line) - 1 - gx if mirror else gx
                        d.rectangle([ox + px * scale, y + gy * scale, ox + (px + 1) * scale - 1, y + (gy + 1) * scale - 1],
                                    fill=color + (a,))
    base = img.convert("RGBA")
    base.alpha_composite(layer)
    return base


def vignette(img, strength=0.45):
    """Darkens the edges a little so the center reads as the stage."""
    w, h = img.size
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).ellipse([-w * 0.25, -h * 0.6, w * 1.25, h * 1.6], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(min(w, h) // 4))
    dark = Image.new("RGBA", (w, h), INK + (int(255 * strength),))
    out = img.copy()
    out.alpha_composite(Image.composite(Image.new("RGBA", (w, h), (0, 0, 0, 0)), dark, mask))
    return out


def background(w, h):
    return vignette(sword_pattern(gradient(w, h)))


# ------------------------------------------------------------------------------------------------ frames and chips

def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


def framed(picture, border=CRIMSON, width=8, radius=22, shadow=True):
    """Picture with rounded corners, a colored border and a soft drop shadow (RGBA, larger than the picture)."""
    w, h = picture.size
    pad = 24
    out = Image.new("RGBA", (w + 2 * width + 2 * pad, h + 2 * width + 2 * pad), (0, 0, 0, 0))
    if shadow:
        sh = Image.new("RGBA", out.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle([pad + 6, pad + 10, pad + w + 2 * width + 6, pad + h + 2 * width + 10],
                                             radius=radius + width, fill=(0, 0, 0, 150))
        out.alpha_composite(sh.filter(ImageFilter.GaussianBlur(12)))
    frame = Image.new("RGBA", (w + 2 * width, h + 2 * width), border + (255,))
    out.paste(frame, (pad, pad), rounded_mask(frame.size, radius + width))
    out.paste(picture.convert("RGBA"), (pad + width, pad + width), rounded_mask((w, h), radius))
    return out


def chip(text, scale=3, fill=(46, 26, 34), ink=BONE, border=CRIMSON):
    tw = text_width(text, scale)
    w, h = tw + 12 * scale, 7 * scale + 8 * scale
    img = Image.new("RGBA", (w + 4, h + 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2, 4, w + 1, h + 3], radius=h // 2, fill=(0, 0, 0, 120))
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=h // 2, fill=fill, outline=border, width=max(2, scale // 2 + 1))
    draw_text(img, text, 6 * scale, 4 * scale, scale, ink, shadow=None)
    return img


# ------------------------------------------------------------------------------------------------ vanilla icons

_JAR = None


def client_jar():
    global _JAR
    if _JAR is None:
        home = os.path.expanduser("~/.gradle/caches")
        candidates = glob.glob(os.path.join(home, "fabric-loom", MC_VERSION, "minecraft-client.jar")) + \
            glob.glob(os.path.join(home, "neoformruntime", "artifacts", f"minecraft_{MC_VERSION}_client.jar"))
        if not candidates:
            raise SystemExit(f"no Minecraft {MC_VERSION} client jar in the Gradle cache: build the fabric module once")
        _JAR = zipfile.ZipFile(candidates[0])
    return _JAR


def icon(path, scale=4):
    """A vanilla texture (e.g. 'item/netherite_sword'), scaled without smoothing."""
    data = client_jar().read(f"assets/minecraft/textures/{path}.png")
    img = Image.open(io.BytesIO(data)).convert("RGBA")
    if img.height > img.width:  # animated strips: first frame
        img = img.crop((0, 0, img.width, img.width))
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


def slot(size):
    """A Minecraft inventory slot (dark bevel top-left, light bottom-right) to sit an icon on."""
    img = Image.new("RGBA", (size, size), (139, 139, 139, 255))
    d = ImageDraw.Draw(img)
    b = max(3, size // 18)
    d.rectangle([0, 0, size - 1, b - 1], fill=(55, 55, 55, 255))
    d.rectangle([0, 0, b - 1, size - 1], fill=(55, 55, 55, 255))
    d.rectangle([0, size - b, size - 1, size - 1], fill=(255, 255, 255, 255))
    d.rectangle([size - b, 0, size - 1, size - 1], fill=(255, 255, 255, 255))
    return img


def mob_face(path, box, scale, overlay=None):
    """A mob face cropped from an entity texture (same UVs as MobTextures), for decoration."""
    data = client_jar().read(f"assets/minecraft/textures/entity/{path}.png")
    img = Image.open(io.BytesIO(data)).convert("RGBA").crop(box)
    if overlay:  # glowing eyes layer (enderman, spider...) drawn over the face, like MobTextures does in game
        eyes = Image.open(io.BytesIO(client_jar().read(f"assets/minecraft/textures/entity/{overlay}.png"))).convert("RGBA")
        img.alpha_composite(eyes.crop(box))
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


# ------------------------------------------------------------------------------------------------ screenshots

def shot(name, required=True):
    files = sorted(glob.glob(os.path.join(SHOTS, f"*{name}*.png")))
    if not files:
        if required:
            raise SystemExit(f"missing screenshot {name}: run KRYLIX_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest")
        return None
    return Image.open(files[-1]).convert("RGB")


def crop_center(img, w, h, cx=0.5, cy=0.5):
    x0 = int(img.width * cx - w / 2)
    y0 = int(img.height * cy - h / 2)
    x0 = max(0, min(img.width - w, x0))
    y0 = max(0, min(img.height - h, y0))
    return img.crop((x0, y0, x0 + w, y0 + h))


def fit(img, w, h, cx=0.5, cy=0.5):
    """Scale to cover w x h, then crop around (cx, cy)."""
    s = max(w / img.width, h / img.height)
    img = img.resize((int(img.width * s + 0.5), int(img.height * s + 0.5)), Image.LANCZOS)
    return crop_center(img, w, h, cx, cy)


def paste(canvas, overlay, x, y):
    canvas.alpha_composite(overlay, (int(x), int(y)))


def placeholder(w, h, label):
    img = Image.new("RGB", (w, h), (36, 30, 40))
    d = ImageDraw.Draw(img)
    for x in range(-h, w, 28):
        d.line([x, h, x + h, 0], fill=(44, 36, 48), width=10)
    draw_text(img, label, (w - text_width(label, 4)) // 2, h // 2 - 14, 4, (120, 110, 125), shadow=None)
    return img


# ------------------------------------------------------------------------------------------------ images

def banner():
    w, h = 1920, 640
    img = background(w, h)
    draw_text(img, "KRYLIX", 76, 92, 22, WHITE, shadow=CRIMSON, outline=CRIMSON_DARK, shadow_step=12)
    draw_text(img, "KILL FEED, STATS & LEADERBOARDS", 82, 290, 5, BONE, shadow=INK)
    x, y = 80, 368
    for label in ("KILL FEED", "HEALTH PLATES", "MOB STATS", "LEADERBOARD", "DEATH RECAP", "13 LANGUAGES"):
        c = chip(label, 3)
        if x + c.width > 1040:
            x, y = 80, y + c.height + 14
        paste(img, c, x, y)
        x += c.width + 14
    x, y = 80, y + 84
    for label, fill, border in ((f"MC {MC_VERSION}", (30, 52, 38), (98, 180, 122)),
                                ("FABRIC", (52, 48, 40), (219, 208, 180)),
                                ("NEOFORGE", (58, 40, 22), (230, 138, 0))):
        c = chip(label, 3, fill=fill, border=border)
        paste(img, c, x, y)
        x += c.width + 14

    hero_shot = shot("docs_hero", required=False)
    # Frame the zombie with its health plate and the kill feed; the left HUD panel falls outside
    hero = fit(hero_shot, 600, 440, cx=0.59, cy=0.42) if hero_shot else placeholder(600, 440, "DOCS HERO")
    frame = framed(hero, border=CRIMSON, width=9)
    frame_x = w - frame.width - 64
    paste(img, frame, frame_x, (h - frame.height) // 2)
    # A column of iconic faces between the text and the picture
    faces = [("zombie/zombie", (8, 8, 16, 16), None), ("skeleton/skeleton", (8, 8, 16, 16), None),
             ("creeper/creeper", (8, 8, 16, 16), None), ("enderman/enderman", (8, 8, 16, 16), "enderman/enderman_eyes")]
    for i, (path, box, eyes) in enumerate(faces):
        face = mob_face(path, box, 9, eyes)
        paste(img, framed(face, border=BONE if i % 2 else GOLD, width=4, radius=8), frame_x - 128, 88 + i * 116)
    return img.convert("RGB")


def header(title, icon_path, accent=CRIMSON):
    w, h = 1600, 132
    img = background(w, h)
    d = ImageDraw.Draw(img)
    d.rectangle([0, h - 8, w, h], fill=accent)
    ic = icon(icon_path, 4 if "sprites" not in icon_path else 6)
    sl = slot(88)
    sy = (h - 8 - sl.height) // 2
    paste(img, sl, 40, sy)
    paste(img, ic, 40 + (sl.width - ic.width) // 2, sy + (sl.height - ic.height) // 2)
    draw_text(img, title, 40 + sl.width + 30, (h - 8 - 7 * 8) // 2, 8, WHITE, shadow=CRIMSON, outline=CRIMSON_DARK)
    return img.convert("RGB")


def gallery(title, cards, cols, card_w, card_h, width=1920):
    """cards: [(image, caption, subcaption, border color)]. Title on top, framed cards with captions below."""
    rows = math.ceil(len(cards) / cols)
    cell_w = card_w + 90
    cell_h = card_h + 190
    h = 170 + rows * cell_h + 20
    img = background(width, h)
    draw_text(img, title, (width - text_width(title, 9)) // 2, 50, 9, WHITE, shadow=INK, outline=CRIMSON_DARK)
    left = (width - cols * cell_w) // 2
    for i, (pic, caption, sub, color) in enumerate(cards):
        col, row = i % cols, i // cols
        x = left + col * cell_w
        y = 170 + row * cell_h
        frame = framed(pic if pic.size == (card_w, card_h) else fit(pic, card_w, card_h), border=color, width=8)
        paste(img, frame, x + (cell_w - frame.width) // 2, y)
        cy = y + frame.height + 4
        draw_text(img, caption, x + (cell_w - text_width(caption, 5)) // 2, cy, 5, color, shadow=INK)
        if sub:
            draw_text(img, sub, x + (cell_w - text_width(sub, 3)) // 2, cy + 50, 3, BONE, shadow=INK)
    return img.convert("RGB")


def ui_crop(img, box, factor=2):
    """Crops a piece of GUI and scales it up without smoothing (the shots use GUI scale 3)."""
    return img.crop(box).resize(((box[2] - box[0]) * factor, (box[3] - box[1]) * factor), Image.NEAREST)


HEADERS = {
    "header_killfeed": ("KILL FEED", "item/netherite_sword"),
    "header_recap": ("DEATH RECAP", "item/totem_of_undying"),
    "header_health": ("HEALTH PLATES", "gui/sprites/hud/heart/full"),
    "header_mobstats": ("MOB STATS", "item/zombie_spawn_egg"),
    "header_leaderboard": ("LEADERBOARD", "item/golden_helmet"),
    "header_config": ("CONFIG & KEYS", "item/comparator"),
    "header_languages": ("LANGUAGES", "item/writable_book"),
    "header_server": ("SERVERS & ADDONS", "item/command_block_minecart"),
    "header_install": ("INSTALLATION", "item/bundle"),
}


def build():
    os.makedirs(OUT, exist_ok=True)
    banner().save(os.path.join(OUT, "banner.png"), optimize=True)
    for name, (title, ic) in HEADERS.items():
        header(title, ic).save(os.path.join(OUT, name + ".png"), optimize=True)

    hero = shot("docs_hero")
    health = shot("docs_health")
    board_pvp = shot("docs_leaderboard_pvp")
    board_mobs = shot("docs_leaderboard_mobs")

    # The HUD up close: the shots use GUI scale 3, doubled without smoothing so the pixels stay square
    hud(ui_crop(hero, (1240, 4, 1912, 248)), ui_crop(hero, (6, 18, 110, 362))).save(os.path.join(OUT, "hud.png"), optimize=True)
    gallery("HEALTH PLATES", [
        (fit(health, 1300, 560, cy=0.3), "HEARTS AND HP OVER WHATEVER YOU AIM AT", None, GOLD),
    ], 1, 1300, 560).save(os.path.join(OUT, "health.png"), optimize=True)
    board = (440, 140, 1480, 940)  # the panel, with a margin of the blurred world around it
    gallery("LEADERBOARD", [
        (board_pvp.crop(board), "PVP", "KILLS, DEATHS, K/D", CRIMSON),
        (board_mobs.crop(board), "MOB KILLS", "WHO CLEARED THE MOST", GOLD),
    ], 2, 780, 600).save(os.path.join(OUT, "leaderboard.png"), optimize=True)
    recap = shot("docs_recap")
    gallery("DEATH RECAP", [
        (recap.crop((430, 130, 1490, 890)), "WHO, WITH WHAT, HOW MUCH HEALTH THEY KEPT", None, BONE),
    ], 1, 1060, 760).save(os.path.join(OUT, "recap.png"), optimize=True)
    print("wrote", sorted(os.listdir(OUT)))


def hud(feed, stats):
    """Kill feed (wide) and mob stats panel (tall) side by side, each framed, with captions."""
    width = 1920
    gap = 70
    frame_feed = framed(feed, border=CRIMSON, width=8)
    frame_stats = framed(stats, border=GOLD, width=8)
    h = 170 + max(frame_feed.height, frame_stats.height) + 90
    img = background(width, h)
    title = "ON YOUR HUD"
    draw_text(img, title, (width - text_width(title, 9)) // 2, 50, 9, WHITE, shadow=INK, outline=CRIMSON_DARK)
    total = frame_stats.width + gap + frame_feed.width
    x = (width - total) // 2
    for frame, caption, sub, color in ((frame_stats, "MOB STATS", "PER WORLD", GOLD),
                                       (frame_feed, "KILL FEED", "CRIT, LONGSHOT, SMASH, HP", CRIMSON)):
        paste(img, frame, x, 160)
        cy = 160 + frame.height + 4
        draw_text(img, caption, x + (frame.width - text_width(caption, 5)) // 2, cy, 5, color, shadow=INK)
        draw_text(img, sub, x + (frame.width - text_width(sub, 3)) // 2, cy + 50, 3, BONE, shadow=INK)
        x += frame.width + gap
    return img.convert("RGB")


def page(base):
    """README with absolute image URLs, for pasting into Modrinth / CurseForge (they can't resolve relative paths)."""
    with open(os.path.join(ROOT, "README.md"), encoding="utf-8") as f:
        text = f.read()
    text = re.sub(r"\]\((docs/images/[^)]+)\)", lambda m: "](" + base.rstrip("/") + "/" + m.group(1) + ")", text)
    raw = re.match(r"https://raw\.githubusercontent\.com/([^/]+)/([^/]+)/([^/]+)", base)
    if raw:
        blob = f"https://github.com/{raw.group(1)}/{raw.group(2)}/blob/{raw.group(3)}/"
        text = re.sub(r"\]\((?!https?://|#|docs/images/)([^)]+)\)", lambda m: "](" + blob + m.group(1) + ")", text)
    out = os.path.join(ROOT, "docs", "pages", "modrinth.md")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    print("wrote", out)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--page-base", help="raw URL of the repo root, e.g. https://raw.githubusercontent.com/eliasnvx/Krylix/<branch>")
    parser.add_argument("--page-only", action="store_true", help="only rewrite docs/pages/modrinth.md")
    args = parser.parse_args()
    if not args.page_only:
        build()
    if args.page_base:
        page(args.page_base)
