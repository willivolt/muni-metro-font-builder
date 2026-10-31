"""Run with FontForge's Python after `mvn verify` from the repository root."""

from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tempfile
import unittest

import fontforge

import build_font


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "build_font.py"


class BuildFontTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.generated = tempfile.TemporaryDirectory(prefix="muni-generated-")
        cls.addClassCleanup(cls.generated.cleanup)
        cls.glyphs = Path(cls.generated.name) / "glyphs"
        subprocess.run(
            ["java", "-jar", str(ROOT / "target" / "muni-metro-font-builder-1.0-SNAPSHOT.jar"),
             str(cls.glyphs)], check=True, capture_output=True, text=True, timeout=60)

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="muni-font-test-")
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.inputs = self.directory / "input glyphs"
        shutil.copytree(self.glyphs, self.inputs)
        self.codepoints = {int(path.stem[1:], 16) for path in self.inputs.glob("*.svg")}
        self.output = self.directory / "output font" / "MuniMetro-Regular.ttf"

    def run_cli(self, *arguments, cwd=ROOT):
        interpreter = [sys.executable]
        if Path(sys.executable).stem.lower() == "fontforge":
            interpreter.extend(["-lang=py", "-script"])
        return subprocess.run([*interpreter, str(SCRIPT), *map(str, arguments)],
                              cwd=cwd, capture_output=True, text=True, timeout=60)

    def assert_cli_failure(self, expected, *arguments):
        result = self.run_cli(*(arguments or (self.inputs, self.output)))
        self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn(expected, result.stderr)
        self.assertNotIn("Generated ", result.stdout)
        self.assertFalse(self.output.exists())

    def test_real_generated_font_metrics_mappings_and_positions(self):
        result = self.run_cli(self.inputs, self.output)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("from 37 SVG glyphs", result.stdout)
        data = self.output.read_bytes()
        self.assertEqual(b"\x00\x01\x00\x00", data[:4])
        table_count = struct.unpack_from(">H", data, 4)[0]
        tables = {tag: offset for tag, _, offset, _ in struct.iter_unpack(
            ">4sIII", data[12:12 + 16 * table_count])}
        self.assertIn(b"glyf", tables)
        self.assertEqual(1, struct.unpack_from(">I", data, tables[b"post"] + 12)[0])
        font = fontforge.open(str(self.output))
        self.addCleanup(font.close)
        self.assertEqual("MuniMetro-Regular", font.fontname)
        self.assertEqual("Muni Metro", font.familyname)
        self.assertEqual("Muni Metro Regular", font.fullname)
        self.assertEqual(1000, font.em)
        self.assertEqual((800, 200), (font.ascent, font.descent))
        self.assertEqual(740, font.os2_capheight)
        self.assertEqual((800, -200, 0),
                         (font.os2_typoascent, font.os2_typodescent, font.os2_typolinegap))
        self.assertEqual((800, -200, 0), (font.hhea_ascent, font.hhea_descent, font.hhea_linegap))
        self.assertEqual((800, 200), (font.os2_winascent, font.os2_windescent))
        mappings = {glyph.unicode for glyph in font.glyphs() if glyph.unicode >= 0}
        mappings.update(codepoint for glyph in font.glyphs()
                        for codepoint, selector, _ in (glyph.altuni or ()) if selector == -1)
        expected = self.codepoints | {
            codepoint + 32 for codepoint in self.codepoints if ord("A") <= codepoint <= ord("Z")
        }
        self.assertEqual(expected | {0, 13}, mappings)
        for codepoint in expected:
            with self.subTest(codepoint=codepoint):
                glyph = font[codepoint]
                self.assertEqual(520, glyph.width)
                self.assertEqual(0, glyph.validate(True))
                self.assertEqual(codepoint != 32, bool(len(glyph.foreground)))
        for character in "A0":
            self.assertEqual((40, 0, 480, 740), font[ord(character)].boundingBox())
        self.assertEqual((190, 0, 330, 740), font[ord("I")].boundingBox())
        self.assertEqual((190, 0, 330, 140), font[ord(".")].boundingBox())
        self.assertEqual((190, 150, 330, 590), font[ord(":")].boundingBox())
        self.assertEqual((40, 300, 480, 440), font[ord("-")].boundingBox())
        self.assertEqual((40, 0, 480, 590), font[ord("/")].boundingBox())
        self.assertTrue(len(font[".notdef"].foreground))
        self.assertEqual(0, font[".notdef"].validate(True))
        self.assertEqual(0, font[".null"].width)
        self.assertEqual(520, font["nonmarkingreturn"].width)
        for text in ("10:45 AM", "FRI 10/29", "2026-10-29", "1.0", "Fri 10:45 am"):
            self.assertEqual(520 * len(text), sum(font[ord(char)].width for char in text))
        # Adjacent full-width glyphs have 80 units between their visible edges.
        self.assertEqual(80, 520 + font[ord("A")].boundingBox()[0]
                         - font[ord("0")].boundingBox()[2])

    def test_lowercase_aliases_share_uppercase_glyphs(self):
        result = self.run_cli(self.inputs, self.output)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        font = fontforge.open(str(self.output))
        self.addCleanup(font.close)
        for lowercase in "abcdefghijklmnopqrstuvwxyz":
            with self.subTest(lowercase=lowercase):
                uppercase = ord(lowercase.upper())
                if uppercase not in self.codepoints:
                    self.assertNotIn(ord(lowercase), font)
                    continue
                lower_glyph = font[ord(lowercase)]
                upper_glyph = font[uppercase]
                self.assertEqual(upper_glyph.originalgid, lower_glyph.originalgid)
                self.assertEqual(upper_glyph.boundingBox(), lower_glyph.boundingBox())
                self.assertEqual(520, lower_glyph.width)

    def test_default_paths_and_regeneration(self):
        target = self.directory / "target"
        target.mkdir()
        shutil.copytree(self.inputs, target / "glyphs")
        for _ in range(2):
            result = self.run_cli(cwd=self.directory)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            self.assertTrue((target / "MuniMetro-Regular.ttf").is_file())

    def test_missing_input_directory(self):
        self.assert_cli_failure("input directory does not exist", self.directory / "missing", self.output)

    def test_removed_artwork_removes_both_case_mappings(self):
        (self.inputs / "U0041.svg").unlink()
        (self.inputs / "U0020.svg").unlink()
        result = self.run_cli(self.inputs, self.output)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        font = fontforge.open(str(self.output))
        self.addCleanup(font.close)
        for codepoint in (32, 65, 97):
            self.assertNotIn(codepoint, font)

    def test_new_artwork_and_supplementary_unicode_are_included(self):
        shutil.copyfile(self.inputs / "U0041.svg", self.inputs / "U005A.svg")
        shutil.copyfile(self.inputs / "U0041.svg", self.inputs / "U1F680.svg")
        result = self.run_cli(self.inputs, self.output)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        font = fontforge.open(str(self.output))
        self.addCleanup(font.close)
        for codepoint in (ord("Z"), ord("z"), 0x1F680):
            self.assertEqual(520, font[codepoint].width)
            self.assertEqual((40, 0, 480, 740), font[codepoint].boundingBox())
        self.assertEqual(font[ord("Z")].originalgid, font[ord("z")].originalgid)

    def test_explicit_lowercase_artwork_takes_precedence_over_alias(self):
        shutil.copyfile(self.inputs / "U002E.svg", self.inputs / "U0061.svg")
        result = self.run_cli(self.inputs, self.output)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        font = fontforge.open(str(self.output))
        self.addCleanup(font.close)
        self.assertNotEqual(font[65].originalgid, font[97].originalgid)
        self.assertEqual((190, 0, 330, 140), font[97].boundingBox())
        self.assertEqual(520, font[97].width)

    def test_empty_input_directory(self):
        for path in self.inputs.iterdir():
            path.unlink()
        (self.inputs / "notes.txt").write_text("not artwork", encoding="utf-8")
        self.assert_cli_failure("No SVG glyph files found")

    def test_font_format_codepoints_are_reserved(self):
        for name in ("U0000.svg", "U000D.svg"):
            with self.subTest(name=name):
                path = self.inputs / name
                shutil.copyfile(self.inputs / "U0041.svg", path)
                try:
                    self.assert_cli_failure("reserved for a font-format glyph")
                finally:
                    path.unlink()

    def test_invalid_filenames_are_rejected(self):
        for name in ("A.svg", "U005a.svg", "U005A.SVG", "U00003A.svg", "U110000.svg", "UD800.svg"):
            with self.subTest(name=name):
                extra = self.inputs / name
                extra.write_text("<svg/>", encoding="utf-8")
                try:
                    self.assert_cli_failure(name)
                finally:
                    extra.unlink()

    def test_non_svg_files_are_ignored(self):
        (self.inputs / "notes.txt").write_text("not artwork", encoding="utf-8")
        self.assertEqual(37, len(build_font.read_inputs(self.inputs)))

    def test_malformed_svg(self):
        (self.inputs / "U0041.svg").write_text("<svg", encoding="utf-8")
        self.assert_cli_failure("Cannot read SVG")

    def test_incompatible_canvas(self):
        path = self.inputs / "U0041.svg"
        path.write_text(path.read_text(encoding="utf-8").replace('width="440.0"', 'width="441.0"'),
                        encoding="utf-8")
        self.assert_cli_failure("expected a 440-by-740 SVG")

    def test_space_must_be_blank(self):
        shutil.copyfile(self.inputs / "U0041.svg", self.inputs / "U0020.svg")
        self.assert_cli_failure("space must contain no outlines")

    def test_empty_non_space_import(self):
        shutil.copyfile(self.inputs / "U0020.svg", self.inputs / "U0041.svg")
        self.assert_cli_failure("import produced no outlines")

    def test_outlines_outside_canvas(self):
        path = self.inputs / "U002E.svg"
        path.write_text(path.read_text(encoding="utf-8").replace(
            'translate(150.0 600.0)', 'translate(450.0 600.0)'), encoding="utf-8")
        self.assert_cli_failure("outlines extend outside the shared canvas")

    def test_non_ttf_output(self):
        self.assert_cli_failure(".ttf extension", self.inputs, self.directory / "font.otf")

    def test_export_failure_preserves_existing_destination(self):
        self.output.mkdir(parents=True)
        sentinel = self.output / "keep.txt"
        sentinel.write_text("unchanged", encoding="utf-8")
        result = self.run_cli(self.inputs, self.output)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Font build failed:", result.stderr)
        self.assertEqual("unchanged", sentinel.read_text(encoding="utf-8"))
        self.assertEqual([self.output], list(self.output.parent.iterdir()))

    def test_invalid_input_preserves_existing_font(self):
        self.output.parent.mkdir()
        self.output.write_bytes(b"existing font")
        (self.inputs / "U0041.svg").write_text("<svg", encoding="utf-8")
        result = self.run_cli(self.inputs, self.output)
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(b"existing font", self.output.read_bytes())


if __name__ == "__main__":
    unittest.main()
