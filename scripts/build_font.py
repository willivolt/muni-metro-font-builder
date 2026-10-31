"""Assemble the Java generator's reviewed SVG repertoire into a TrueType font."""

import argparse
import os
from pathlib import Path
import re
import sys
import tempfile
import xml.etree.ElementTree as ET

try:
    import fontforge
    import psMat
except ImportError as error:
    sys.exit(f"FontForge Python modules are required; run with FontForge's Python: {error}")


CANVAS_WIDTH = 440
CANVAS_HEIGHT = 740
ASCENT = 800
DESCENT = 200
SIDE_BEARING = 40
ADVANCE = CANVAS_WIDTH + 2 * SIDE_BEARING
SVG_NS = "{http://www.w3.org/2000/svg}"


class BuildError(Exception):
    """An input or FontForge operation prevented a complete font build."""


def read_inputs(directory):
    if not directory.is_dir():
        raise BuildError(f"SVG input directory does not exist: {directory}")
    inputs = {}
    for path in sorted(directory.iterdir()):
        if path.suffix.lower() != ".svg":
            continue
        if not path.is_file() or not re.fullmatch(r"U[0-9A-F]{4,6}\.svg", path.name):
            raise BuildError(f"Invalid SVG filename: {path.name}; expected UXXXX.svg")
        codepoint = int(path.stem[1:], 16)
        if (path.name != f"U{codepoint:04X}.svg" or codepoint > 0x10FFFF
                or 0xD800 <= codepoint <= 0xDFFF):
            raise BuildError(f"Invalid Unicode SVG filename: {path.name}")
        if codepoint in (0, 13):
            raise BuildError(f"{path.name}: code point is reserved for a font-format glyph")
        inputs[codepoint] = path
    if not inputs:
        raise BuildError(f"No SVG glyph files found in: {directory}")
    for codepoint, path in inputs.items():
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError) as error:
            raise BuildError(f"Cannot read SVG {path}: {error}") from error
        canvas_error = f"{path}: expected a 440-by-740 SVG with viewBox='0 0 440 740'"
        try:
            dimensions = (float(root.get("width", "nan")), float(root.get("height", "nan")))
            viewbox = tuple(map(float, root.get("viewBox", "").split()))
        except ValueError as error:
            raise BuildError(canvas_error) from error
        if (root.tag != SVG_NS + "svg" or dimensions != (CANVAS_WIDTH, CANVAS_HEIGHT)
                or viewbox != (0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)):
            raise BuildError(canvas_error)
        if codepoint == 32 and contains_drawing_elements(root):
            raise BuildError(f"{path}: space must contain no outlines")
    return inputs


def contains_drawing_elements(element):
    non_rendering = {"title", "desc", "metadata"}
    for child in element:
        local_name = child.tag.rsplit("}", 1)[-1]
        if local_name in non_rendering:
            continue
        if child.tag != SVG_NS + "g":
            return True
        if contains_drawing_elements(child):
            return True
    return False


def configure_font(font):
    font.encoding = "UnicodeFull"
    font.em = ASCENT + DESCENT
    font.ascent = ASCENT
    font.descent = DESCENT
    font.fontname = "MuniMetro-Regular"
    font.familyname = "Muni Metro"
    font.fullname = "Muni Metro Regular"
    font.weight = "Regular"
    font.version = "1.0"
    font.os2_weight = 400
    font.os2_width = 5
    font.os2_capheight = CANVAS_HEIGHT
    font.os2_typoascent = font.hhea_ascent = font.os2_winascent = ASCENT
    font.os2_typodescent = font.hhea_descent = -DESCENT
    font.os2_windescent = DESCENT
    font.os2_typoascent_add = font.os2_typodescent_add = False
    font.hhea_ascent_add = font.hhea_descent_add = False
    font.os2_winascent_add = font.os2_windescent_add = False
    font.os2_typolinegap = font.hhea_linegap = 0
    font.os2_use_typo_metrics = True


def clean_glyph(glyph):
    glyph.removeOverlap()
    glyph.correctDirection()
    glyph.addExtrema()
    glyph.round()
    validation = glyph.validate(True)
    if validation:
        raise BuildError(f"{glyph.glyphname}: invalid contours (FontForge flags 0x{validation:X})")


def add_format_glyphs(font):
    missing = font.createChar(-1, ".notdef")
    pen = missing.glyphPen()
    for points in (
            ((40, 0), (480, 0), (480, 740), (40, 740)),
            ((80, 40), (80, 700), (440, 700), (440, 40))):
        pen.moveTo(points[0])
        for point in points[1:]:
            pen.lineTo(point)
        pen.closePath()
    del pen
    missing.width = ADVANCE
    clean_glyph(missing)
    font.createChar(0, ".null").width = 0
    font.createChar(13, "nonmarkingreturn").width = ADVANCE


def build_font(input_directory, output):
    inputs = read_inputs(input_directory)
    lowercase_aliases = {
        codepoint + 32: codepoint for codepoint in inputs
        if ord("A") <= codepoint <= ord("Z") and codepoint + 32 not in inputs
    }
    if output.suffix.lower() != ".ttf":
        raise BuildError(f"Output must have a .ttf extension: {output}")
    font = fontforge.font()
    try:
        configure_font(font)
        add_format_glyphs(font)
        for codepoint, path in inputs.items():
            glyph = font.createChar(codepoint)
            if codepoint != 32:
                try:
                    glyph.importOutlines(str(path), scale=False)
                    if not len(glyph.foreground):
                        raise BuildError(f"{path}: import produced no outlines")
                    # FontForge maps SVG y to ascent-y; align the shared canvas bottom to y=0.
                    glyph.transform(psMat.translate(SIDE_BEARING, CANVAS_HEIGHT - ASCENT))
                    clean_glyph(glyph)
                except (OSError, ValueError, RuntimeError) as error:
                    raise BuildError(f"Cannot import SVG {path}: {error}") from error
                left, bottom, right, top = glyph.boundingBox()
                if left < SIDE_BEARING or bottom < 0 or right > ADVANCE - SIDE_BEARING or top > CANVAS_HEIGHT:
                    raise BuildError(f"{path}: outlines extend outside the shared canvas")
            glyph.width = ADVANCE
        for lowercase, uppercase in lowercase_aliases.items():
            font[uppercase].altuni = ((lowercase, -1, 0),)
        output.parent.mkdir(parents=True, exist_ok=True)
        # Never replace an existing font with a partial or invalid export.
        with tempfile.TemporaryDirectory(prefix="muni-font-", dir=output.parent) as temporary:
            exported = Path(temporary) / output.name
            font.generate(str(exported))
            verify_export(exported, inputs.keys(), lowercase_aliases)
            os.replace(exported, output)
    finally:
        font.close()
    return len(inputs)


def verify_export(path, codepoints, lowercase_aliases):
    exported = fontforge.open(str(path))
    try:
        for codepoint in set(codepoints) | lowercase_aliases.keys():
            if codepoint not in exported or exported[codepoint].width != ADVANCE:
                raise BuildError(f"Export lost Unicode mapping or advance for U{codepoint:04X}")
            glyph = exported[codepoint]
            if codepoint == 32:
                if len(glyph.foreground):
                    raise BuildError("Exported space unexpectedly contains outlines")
            elif not len(glyph.foreground) or glyph.validate(True):
                raise BuildError(f"Invalid exported contours for U{codepoint:04X}")
        for lowercase, uppercase in lowercase_aliases.items():
            if exported[lowercase].originalgid != exported[uppercase].originalgid:
                raise BuildError(f"Export lost uppercase glyph alias for U{lowercase:04X}")
        if ".notdef" not in exported or not len(exported[".notdef"].foreground):
            raise BuildError("Export is missing the .notdef glyph")
    finally:
        exported.close()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input_directory", nargs="?", type=Path, default=Path("target/glyphs"),
                        help="generated SVG directory (default: target/glyphs)")
    parser.add_argument("output", nargs="?", type=Path, default=Path("target/MuniMetro-Regular.ttf"),
                        help="TrueType output path (default: target/MuniMetro-Regular.ttf)")
    arguments = parser.parse_args(argv)
    try:
        count = build_font(arguments.input_directory, arguments.output)
    except (BuildError, OSError, ValueError, RuntimeError) as error:
        print(f"Font build failed: {error}", file=sys.stderr)
        return 1
    print(f"Generated {arguments.output} from {count} SVG glyphs.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
