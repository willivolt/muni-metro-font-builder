package com.willivolt.munimetro.fontcreator;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class SvgGlyphWriterTest {
    @TempDir
    Path temporary;

    @Test
    void rendersTheCanvasAndRowMajorTranslationsWithoutBlankCells() throws Exception {
        CellShape[] cells = new CellShape[15];
        Arrays.fill(cells, CellShape.EMPTY);
        cells[0] = CellShape.BLOCK;
        cells[7] = CellShape.ARC_BR;
        cells[14] = CellShape.TRI_H_TL;
        String content = new SvgGlyphWriter(GridSpec.DEFAULT).render(new GlyphDefinition('A', cells));
        Element svg = SvgTestSupport.parse(content);
        assertEquals("http://www.w3.org/2000/svg", svg.getNamespaceURI());
        assertEquals("440.0", svg.getAttribute("width"));
        assertEquals("740.0", svg.getAttribute("height"));
        assertEquals("0 0 440.0 740.0", svg.getAttribute("viewBox"));
        Element group = (Element) svg.getElementsByTagName("g").item(0);
        assertEquals("black", group.getAttribute("fill"));
        assertEquals("none", group.getAttribute("stroke"));
        NodeList groups = svg.getElementsByTagName("g");
        assertEquals(4, groups.getLength());
        assertCell((Element) groups.item(1), "0", "BLOCK", "translate(0.0 0.0)");
        assertCell((Element) groups.item(2), "7", "ARC_BR", "translate(150.0 300.0)");
        assertCell((Element) groups.item(3), "14", "TRI_H_TL", "translate(300.0 600.0)");
        assertEquals(1, svg.getElementsByTagName("rect").getLength());
        assertFalse(content.contains("EMPTY"));
    }

    @Test
    void combinesPrimitivesWithinOneTranslatedCellAndOmitsBlankCombinations() throws Exception {
        CellPattern[] cells = new CellPattern[15];
        Arrays.fill(cells, CellShape.EMPTY);
        cells[4] = CellPattern.combine(CellShape.TRI_Q_UP, CellShape.TRI_Q_LEFT, CellShape.TRI_Q_RIGHT);
        cells[7] = CellShape.TRI_Q_DOWN;
        cells[14] = CellPattern.combine(CellShape.EMPTY, CellShape.EMPTY);
        Element svg = SvgTestSupport.parse(new SvgGlyphWriter(GridSpec.DEFAULT)
                .render(new GlyphDefinition('M', cells)));
        NodeList groups = svg.getElementsByTagName("g");
        assertEquals(3, groups.getLength());
        Element combinedCell = (Element) groups.item(1);
        assertCell(combinedCell, "4", "TRI_Q_UP+TRI_Q_LEFT+TRI_Q_RIGHT", "translate(150.0 150.0)");
        assertEquals(3, combinedCell.getElementsByTagName("polygon").getLength());
        assertCell((Element) groups.item(2), "7", "TRI_Q_DOWN", "translate(150.0 300.0)");
        assertEquals(4, svg.getElementsByTagName("polygon").getLength());
    }

    @Test
    void rendersNotchesAsSingleNamedPolygons() throws Exception {
        CellPattern[] cells = new CellPattern[15];
        Arrays.fill(cells, CellShape.EMPTY);
        cells[4] = CellShape.NOTCH_DOWN;
        Element svg = SvgTestSupport.parse(new SvgGlyphWriter(GridSpec.DEFAULT)
                .render(new GlyphDefinition('M', cells)));
        Element notchCell = (Element) svg.getElementsByTagName("g").item(1);
        assertCell(notchCell, "4", "NOTCH_DOWN", "translate(150.0 150.0)");
        assertEquals(1, notchCell.getElementsByTagName("polygon").getLength());
    }

    @Test
    void supportsBlankGlyphsAndConfiguredGridSizes() throws Exception {
        Element svg = SvgTestSupport.parse(new SvgGlyphWriter(new GridSpec(4, 1))
                .render(new GlyphDefinition(' ', java.util.Collections.nCopies(15, CellShape.EMPTY))));
        assertEquals("14.0", svg.getAttribute("width"));
        assertEquals("24.0", svg.getAttribute("height"));
        assertEquals(1, svg.getElementsByTagName("g").getLength());
        assertEquals(0, svg.getElementsByTagName("rect").getLength());
    }

    @Test
    void numericOutputAndFilenamesAreIndependentOfDefaultLocale() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'M').findFirst().orElseThrow();
        SvgGlyphWriter writer = new SvgGlyphWriter(new GridSpec(140.25, 10.5));
        String expected = writer.render(glyph);
        String filename = glyph.filename();
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals(expected, writer.render(glyph));
            assertEquals(filename, glyph.filename());
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));
            assertEquals(expected, writer.render(glyph));
            assertEquals(filename, glyph.filename());
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void createsDirectoriesOverwritesMatchingFilesAndPreservesUnrelatedFiles() throws Exception {
        Path directory = temporary.resolve("nested").resolve("glyphs");
        SvgGlyphWriter writer = new SvgGlyphWriter(GridSpec.DEFAULT);
        GlyphDefinition glyph = GlyphLibrary.glyphs().getFirst();
        Path file = writer.write(directory, glyph);
        assertEquals(directory.resolve("U0030.svg"), file);
        assertEquals(writer.render(glyph), Files.readString(file, StandardCharsets.UTF_8));
        Path unrelated = Files.writeString(directory.resolve("review.txt"), "Keep this");
        Files.writeString(file, "old SVG");
        writer.write(directory, glyph);
        assertEquals(writer.render(glyph), Files.readString(file));
        assertEquals("Keep this", Files.readString(unrelated));
    }

    @Test
    void everyLibraryGlyphProducesValidXml() throws Exception {
        SvgGlyphWriter writer = new SvgGlyphWriter(GridSpec.DEFAULT);
        for (GlyphDefinition glyph : GlyphLibrary.glyphs()) {
            Element svg = SvgTestSupport.parse(writer.render(glyph));
            long nonempty = glyph.cells().stream().filter(shape -> !shape.svg(GridSpec.DEFAULT).isEmpty()).count();
            assertEquals(nonempty + 1, svg.getElementsByTagName("g").getLength(), glyph.filename());
        }
    }

    @Test
    void outputErrorsArePropagated() throws Exception {
        Path file = Files.writeString(temporary.resolve("not-a-directory"), "existing");
        SvgGlyphWriter writer = new SvgGlyphWriter(GridSpec.DEFAULT);
        assertThrows(java.io.IOException.class, () -> writer.write(file, GlyphLibrary.glyphs().getFirst()));
        assertEquals("existing", Files.readString(file));
        assertThrows(NullPointerException.class, () -> new SvgGlyphWriter(null));
    }

    private static void assertCell(Element cell, String index, String shape, String translation) {
        assertEquals(index, cell.getAttribute("data-cell"));
        assertEquals(shape, cell.getAttribute("data-shape"));
        assertEquals(translation, cell.getAttribute("transform"));
    }
}
