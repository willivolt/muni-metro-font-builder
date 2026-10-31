# muni-metro-font-builder
Creator for font inspired by MUNI Metro Breda train signs

## Requirements

- JDK 21
- Maven 3.9 or later
- For font assembly: FontForge with its Python 3 scripting modules (`fontforge`
  and `psMat`). These are supplied by FontForge, not installed with `pip`.

## Generate and review SVGs

```shell
mvn verify
java -jar target/muni-metro-font-builder-1.0-SNAPSHOT.jar
```

This generates 37 individual SVGs under `target/glyphs`. Open them in a browser
or an SVG preview in VS Code to inspect the shapes. The initial library contains
digits `0` through `9`, a blank space, punctuation `: / - .`, and letters
`A B C D E F G H I J L M N O P R S T U V W Y`.

To keep review files outside Maven's build directory, pass an output directory:

```shell
java -jar target/muni-metro-font-builder-1.0-SNAPSHOT.jar "review glyphs"
```

The directory is created if needed. Each run overwrites matching glyph files
and leaves unrelated files alone, including SVGs for characters you have removed
from the library. `mvn clean` deletes `target`, including the default glyph output.
Invalid arguments or file-write failures produce a nonzero exit status.

Filenames identify the character's Unicode value: `U0030.svg` is `0`,
`U0041.svg` is `A`, `U003A.svg` is `:`, `U002F.svg` is `/`, and `U002D.svg`
is `-`. This keeps punctuation filenames valid on Windows.

Space is `U0020.svg` and has no visible shapes. The period is `U002E.svg` and
starts as a single bottom-center block.

See the [glyph filename reference](GLYPH-REFERENCE.md) for the full character,
Unicode, and filename mapping.

## Assemble a TrueType font

Run commands from the repository root. Generate the SVGs first, then use
FontForge's Python interpreter (ordinary system Python usually cannot import
`fontforge`):

```shell
fontforge -lang=py -script scripts/build_font.py
fontforge -lang=py -script scripts/build_font.py "review glyphs" "target/MuniMetro-Regular.ttf"
```

The optional positional arguments are the SVG directory (default `target/glyphs`)
and `.ttf` output path (default `target/MuniMetro-Regular.ttf`), relative to the
current directory. Parent output directories are created as needed. The script
reopens and checks the exported font before replacing an existing output file;
failures print a diagnostic and exit nonzero without replacing it.

On Windows, the FontForgeBuilds distribution at `C:\Program Files\FontForgeBuilds`
includes a wrapper that configures its bundled Python and DLL paths:

```powershell
& "C:\Program Files\FontForgeBuilds\fontforge.bat" -lang=py -script scripts\build_font.py
& "C:\Program Files\FontForgeBuilds\fontforge.bat" -lang=py -script scripts\build_font.py "review glyphs" "target\MuniMetro-Regular.ttf"
```

Do not pass script arguments to `fontforge-console.bat`: it opens an interactive
shell and does not forward them. Alternatively, open that console, change to the
repository root, and run `ffpython scripts\build_font.py`.

The font is **Muni Metro Regular**, PostScript name `MuniMetro-Regular`, version
1.0. Metrics are fixed to the reviewed default grid:

| Metric | Font units |
|---|---:|
| Units per em | 1000 |
| Ascent / descent | 800 / 200 |
| Cap height / artwork height | 740 |
| Artwork width | 440 |
| Left / right canvas side bearing | 40 / 40 |
| Monospaced advance (including space) | 520 |
| Line gap | 0 |

SVG `(x, y)` maps to font `(x + 40, 740 - y)` without scaling or fitting visible
bounds. The canvas bottom is the baseline; narrow letters and punctuation retain
their grid positions. For example, period occupies x=190..330, y=0..140, whereas
colon occupies x=190..330, y=150..590. Actual side bearings for narrow glyphs
therefore include their blank canvas columns. Adjacent full-width glyphs have
80 units of inter-character space; the 10-unit internal cell gaps are unchanged.
Typographic, Windows, and horizontal-header line metrics are set explicitly.

The SVG files present in the input directory define the repertoire; there is no
hard-coded character list. Each canonical uppercase `UXXXX.svg` filename maps
to its Unicode scalar value (four to six hex digits, for example `U0041.svg` or
`U1F680.svg`). Adding or removing artwork files adds or removes their mappings.
Non-SVG files are ignored. Empty input directories, misnamed files, invalid
Unicode values, non-default canvases, empty visible glyphs, and nonblank space
artwork are errors. Space is included only if `U0020.svg` is present.

**All valid SVGs are included, even stale files left by the Java generator.**
Remove unwanted SVGs or generate into a clean directory before assembling.
The font also contains a visible `.notdef` box, a zero-width `.null` (U+0000),
and a blank `nonmarkingreturn` (U+000D). Those two code points are reserved;
`U0000.svg` and `U000D.svg` are rejected.

Lowercase ASCII letters map to the same glyphs as their supported uppercase
counterparts, so `Fri 10:45 am` renders in the all-caps design with unchanged
monospaced spacing. These are additional Unicode mappings, not duplicate
outlines or SVG files. Aliases are derived from the ASCII uppercase SVGs found;
letters without artwork remain unsupported. An explicit lowercase SVG takes
precedence over an alias, allowing distinct lowercase artwork without losing it.

Contours are converted to quadratic outlines, overlaps removed, directions
corrected, extrema added, and coordinates rounded to integer font units.
FontForge contour validation must pass both before export and after reopening
the TrueType font. Pebble resource generation is not included.

## Editing character arrays

Edit `GlyphLibrary.java` in the `com.willivolt.munimetro.fontcreator` package.
Each `GlyphDefinition` contains exactly 15 typed cell patterns, arranged as five
rows of three cells. Each pattern is a `CellShape` primitive or a combination
of primitives. For example, `I` starts as:

```java
new GlyphDefinition('I',
        EMPTY, BLOCK, EMPTY,
        EMPTY, BLOCK, EMPTY,
        EMPTY, BLOCK, EMPTY,
        EMPTY, BLOCK, EMPTY,
        EMPTY, BLOCK, EMPTY)
```

Use `combine(...)` to draw multiple primitives in the same cell. With
`import static com.willivolt.munimetro.fontcreator.CellPattern.combine;`,
the inverse of `TRI_Q_DOWN` is:

```java
combine(TRI_Q_UP, TRI_Q_LEFT, TRI_Q_RIGHT)
```

This fills the cell except for a downward-pointing triangular notch from the
top edge. A combination occupies one array entry, just like a single primitive;
it adds the filled regions without erasing any of them. Use `EMPTY` for a blank
cell rather than calling `combine()` without shapes.

For `M`, columns 1 and 3 are solid `BLOCK` cells, cell 5 uses this combination,
and cell 8 uses `TRI_Q_DOWN`. Cell numbers in artwork discussions are 1-based,
read left-to-right and top-to-bottom; Java list indices remain 0-based.

Change the entries, rebuild with `mvn verify`, rerun the JAR, and refresh the SVG
preview. The initial arrays are starting artwork for visual refinement, not final
font designs. The Java tool generates SVGs only; font assembly is a separate step.

`GridSpec.DEFAULT` controls cell size and spacing for all glyphs. The defaults
are 140-unit square cells and 10-unit gaps, yielding a 440-by-740 canvas.
SVGs have black fills, no strokes, and transparent backgrounds.

| Shape identifiers | Meaning |
|---|---|
| `EMPTY`, `BLOCK` | Transparent cell or full square |
| `ARC_TL`, `ARC_TR`, `ARC_BL`, `ARC_BR` | Quarter disks centered at the named corner, with radius equal to the cell size |
| `TRI_H_TL`, `TRI_H_TR`, `TRI_H_BL`, `TRI_H_BR` | Half-square triangles filling the named corner |
| `TRI_Q_UP`, `TRI_Q_DOWN`, `TRI_Q_LEFT`, `TRI_Q_RIGHT` | Quarter-square triangles with the center tip pointing in the named direction |
| `TRI_2V_TOP_L`, `TRI_2V_TOP_R` | Upper trapezoids of a two-cell vertical triangle with a left or right vertical edge |
| `TRI_2V_BOT_L`, `TRI_2V_BOT_R` | Lower triangle fragments with a bottom-left or bottom-right point |

For rounded **outside** corners, the circle center is the opposite corner:
top-left uses `ARC_BR`, top-right uses `ARC_BL`, bottom-left uses `ARC_TR`,
and bottom-right uses `ARC_TL`. For example, the bottom of `U` is
`ARC_TR, BLOCK, ARC_TL`, not `ARC_BL, BLOCK, ARC_BR`.

The artwork convention is to use quarter circles only in the four corner
cells: 1, 3, 13, and 15 (1-based). Interior cells should use blocks or triangles,
including combinations where needed. Some initial glyphs still have interior
arcs pending individual artwork review.

Two-cell fragments follow a shared diagonal across the intervening gap. For a
continuous triangle, place the matching top and bottom fragments in adjacent
rows of the same column. Fragments can also be used independently.

## Tests

```shell
mvn test
mvn "-Dtest=CellShapeTest" test
mvn "-Dtest=FontCreatorTest#generatesAllGlyphsAndCanRegenerateThem" test
```

After `mvn verify`, run the font integration tests with FontForge's Python:

```shell
fontforge -lang=py -script scripts/test_build_font.py
```

```powershell
& "C:\Program Files\FontForgeBuilds\fontforge.bat" -lang=py -script scripts\test_build_font.py
```

These tests generate fresh SVGs using the packaged Java application, build and
reopen real TrueType fonts in temporary directories, check mappings, geometry,
metrics and spacing in time/date strings, and exercise CLI failure behavior.
