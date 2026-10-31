# Repository instructions

## Build and test

Use JDK 21 and Maven 3.9 or later. Run commands from the repository root:

```powershell
mvn verify
mvn test
mvn "-Dtest=FontCreatorTest" test
mvn "-Dtest=FontCreatorTest#generatesAllGlyphsAndCanRegenerateThem" test
mvn "-Dtest=CellShapeTest,SvgGlyphWriterTest" test
java -jar target\muni-metro-font-builder-1.0-SNAPSHOT.jar
java -jar target\muni-metro-font-builder-1.0-SNAPSHOT.jar "review glyphs"
```

`mvn verify` runs tests and packages the executable JAR. Use `mvn clean verify`
after renaming classes to remove stale compiled classes and test reports.

## Architecture

This is a single-module Maven Java application that converts typed 3-by-5
character arrays into individual SVG glyphs inspired by MUNI Metro Breda train
signs. `scripts/build_font.py` separately assembles the reviewed SVGs into a
TrueType font using FontForge's Python API; Pebble resource generation is outside
the current scope. See README.md for FontForge invocation and fixed metrics.
After `mvn verify`, run `fontforge -lang=py -script scripts/test_build_font.py`
for the real-font integration tests (on Windows, use FontForge's `fontforge.bat`
wrapper). The script derives its repertoire from all canonical Unicode-named
SVGs in the input directory, including stale files; keep that directory clean.
ASCII lowercase aliases are derived from uppercase artwork unless an explicit
lowercase SVG is present.

The application entry point is
`com.willivolt.munimetro.fontcreator.FontCreator`. The Maven JAR plugin references
that fully qualified name in the manifest, allowing the packaged application to
run with `java -jar`. Keep the manifest synchronized with entry-point renames.

`GlyphLibrary` holds editable five-row character definitions. `GlyphDefinition`
copies and validates their 15 cell entries. `GridSpec.DEFAULT` sets the shared
cell size and gap; `CellShape` generates local primitive geometry.
`CellPattern` accepts either a primitive or an immutable combination of primitives;
`CellPattern.combine(...)` draws all components at the same local origin.
`SvgGlyphWriter` translates it to grid positions and writes SVG documents.
`FontCreator` generates the whole library, using `target\glyphs` by default or
one optional output-directory argument.

Production and test code use the `com.willivolt.munimetro.fontcreator` base
package under Maven's standard source roots. JUnit Jupiter is a test-scoped
dependency; the application currently has no runtime dependencies.

## Conventions

- Use `FontCreator` and `FontCreatorTest` for the entry point and its test,
  rather than the archetype names `App` and `AppTest`.
- Compiler release and source encoding are configured in `pom.xml` as Java 21
  and UTF-8.
- Patterns are row-major: five rows of three entries, each a `CellShape` or
  `CellPattern.combine(...)`. A combination counts as one cell and adds filled
  regions, not subtracting them. Keep Java layout readable for artwork edits.
- Use 1-based cell numbers (1-15) in artwork discussions; Java indices and
  generated SVG `data-cell` attributes remain 0-based.
- Default cells are 140 units with 10-unit gaps, producing a 440-by-740 canvas.
  ARC suffixes name the circle center; TRI_H suffixes name the filled corner;
  TRI_Q suffixes name the tip direction. A rounded outer corner uses the
  opposite ARC center (for example, top-left rounding uses ARC_BR).
  Quarter circles belong only in cells 1, 3, 13, and 15 (1-based); remaining
  interior arcs in seed artwork need individual review, not blind replacement.
  Two-cell fragments must use a shared
  diagonal spanning two cells plus the gap, not independent half-cell cuts.
- SVG numbers are locale-independent, files use UTF-8, and geometry has black
  fills, no strokes, and transparent backgrounds.
- Glyph filenames use uppercase Unicode values, such as U0041.svg for A and
  U003A.svg for colon, rather than raw punctuation characters.
- Regeneration overwrites matching glyph files but does not delete unrelated or
  stale files. A success count is printed only after all writes complete.
- Tests use temporary directories; CLI failure tests run a subprocess to verify
  nonzero exits without terminating the test JVM.
- Maven build output belongs in the ignored `target` directory.
