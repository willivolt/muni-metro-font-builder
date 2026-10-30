package com.willivolt.munimetro.fontcreator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GlyphDefinitionTest {
    @Test
    void copiesArraysAndExposesImmutableCells() {
        CellShape[] cells = new CellShape[15];
        Arrays.fill(cells, CellShape.EMPTY);
        GlyphDefinition glyph = new GlyphDefinition('A', cells);
        cells[0] = CellShape.BLOCK;
        assertEquals(CellShape.EMPTY, glyph.cells().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> glyph.cells().set(0, CellShape.BLOCK));
    }

    @Test
    void copiesLists() {
        List<CellPattern> cells = new ArrayList<>(java.util.Collections.nCopies(15, CellShape.EMPTY));
        GlyphDefinition glyph = new GlyphDefinition('A', cells);
        cells.set(0, CellShape.BLOCK);
        assertEquals(CellShape.EMPTY, glyph.cells().getFirst());
    }

    @Test
    void acceptsMixedPrimitiveAndCombinedCellsAndCopiesTheArray() {
        CellPattern[] cells = new CellPattern[15];
        Arrays.fill(cells, CellShape.EMPTY);
        CellPattern combined = CellPattern.combine(CellShape.TRI_Q_UP, CellShape.TRI_Q_LEFT, CellShape.TRI_Q_RIGHT);
        cells[4] = combined;
        cells[7] = CellShape.TRI_Q_DOWN;
        GlyphDefinition glyph = new GlyphDefinition('M', cells);
        cells[4] = CellShape.BLOCK;
        assertEquals(15, glyph.cells().size());
        assertEquals(combined, glyph.cells().get(4));
        assertEquals(CellShape.TRI_Q_DOWN, glyph.cells().get(7));
    }

    @Test
    void rejectsWrongLengthNullCellsAndSurrogates() {
        assertThrows(IllegalArgumentException.class, () -> new GlyphDefinition('A', new CellShape[14]));
        assertThrows(IllegalArgumentException.class,
                () -> new GlyphDefinition('A', java.util.Collections.nCopies(16, CellShape.EMPTY)));
        assertThrows(NullPointerException.class, () -> new GlyphDefinition('A', (CellShape[]) null));
        assertThrows(NullPointerException.class, () -> new GlyphDefinition('A', new CellShape[15]));
        assertThrows(IllegalArgumentException.class,
                () -> new GlyphDefinition('\uD800', java.util.Collections.nCopies(15, CellShape.EMPTY)));
    }

    @Test
    void usesUnicodeFilenamesForLettersAndPunctuation() {
        List<CellPattern> blank = java.util.Collections.nCopies(15, CellShape.EMPTY);
        assertEquals("U0041.svg", new GlyphDefinition('A', blank).filename());
        assertEquals("U003A.svg", new GlyphDefinition(':', blank).filename());
        assertEquals("U002F.svg", new GlyphDefinition('/', blank).filename());
        assertEquals("U002D.svg", new GlyphDefinition('-', blank).filename());
    }
}
