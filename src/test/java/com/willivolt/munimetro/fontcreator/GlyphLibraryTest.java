package com.willivolt.munimetro.fontcreator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlyphLibraryTest {
    @Test
    void containsExactlyTheRequestedCharactersInStableOrder() {
        List<GlyphDefinition> glyphs = GlyphLibrary.glyphs();
        assertEquals(37, glyphs.size());
        assertEquals("0123456789:/- .ABCDEFGHIJLMNOPRSTUVWY",
                glyphs.stream().map(glyph -> String.valueOf(glyph.character())).collect(Collectors.joining()));
        assertEquals(37, glyphs.stream().map(GlyphDefinition::filename).distinct().count());
        for (GlyphDefinition glyph : glyphs) {
            assertEquals(15, glyph.cells().size());
            assertTrue(glyph.cells().stream().allMatch(java.util.Objects::nonNull));
            assertTrue(glyph.filename().matches("U[0-9A-F]{4}\\.svg"));
        }
        assertEquals(glyphs, GlyphLibrary.glyphs());
        assertThrows(UnsupportedOperationException.class, () -> glyphs.removeFirst());
    }

    @ParameterizedTest
    @CsvSource({
            "0,1,ARC_BR", "0,3,ARC_BL", "0,13,ARC_TR", "0,15,ARC_TL",
            "2,1,ARC_BR", "2,3,ARC_BL", "2,7,ARC_BR", "2,9,ARC_TL",
            "3,1,ARC_BR", "3,3,ARC_BL", "3,13,ARC_TR", "3,15,ARC_TL",
            "5,13,ARC_TR", "5,15,ARC_TL",
            "6,1,ARC_BR", "6,3,ARC_BL", "6,13,ARC_TR", "6,15,ARC_TL",
            "8,1,ARC_BR", "8,3,ARC_BL", "8,13,ARC_TR", "8,15,ARC_TL",
            "9,1,ARC_BR", "9,3,ARC_BL", "9,7,ARC_TR", "9,13,ARC_TR", "9,15,ARC_TL",
            "A,1,ARC_BR", "A,3,ARC_BL",
            "B,3,ARC_BL", "B,15,ARC_TL",
            "C,1,ARC_BR", "C,3,ARC_BL", "C,13,ARC_TR", "C,15,ARC_TL",
            "D,3,ARC_BL", "D,15,ARC_TL",
            "G,1,ARC_BR", "G,3,ARC_BL", "G,13,ARC_TR", "G,15,ARC_TL",
            "O,1,ARC_BR", "O,3,ARC_BL", "O,13,ARC_TR", "O,15,ARC_TL",
            "P,3,ARC_BL",
            "R,3,ARC_BL",
            "S,1,ARC_BR", "S,3,ARC_BL", "S,13,ARC_TR", "S,15,ARC_TL",
            "W,13,ARC_TR", "W,15,ARC_TL",
            "U,13,ARC_TR", "U,15,ARC_TL"
    })
    void roundedCornersUseTheOppositeCircleCenter(char character, int cell, CellShape expected) {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == character).findFirst().orElseThrow();
        assertEquals(expected, glyph.cells().get(cell - 1), character + " cell " + cell);
    }

    @ParameterizedTest
    @CsvSource({
            "4,7,TRI_H_TR",
            "5,7,TRI_H_TR", "5,9,TRI_H_BL",
            "6,9,TRI_H_BL",
            "S,7,TRI_H_TR", "S,9,TRI_H_BL",
            "P,9,TRI_H_TL", "G,8,TRI_Q_LEFT",
            "V,10,TRI_2V_TOP_R", "V,12,TRI_2V_TOP_L",
            "V,13,TRI_2V_BOT_R", "V,15,TRI_2V_BOT_L",
            "Y,7,TRI_H_TR", "Y,9,TRI_H_TL"
    })
    void diagonalTipsPointTowardTheNextPartOfTheStroke(char character, int cell, CellShape expected) {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == character).findFirst().orElseThrow();
        assertEquals(expected, glyph.cells().get(cell - 1), character + " cell " + cell);
    }

    @Test
    void yUsesLoweredArmsAndADownwardNotchInCell8() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'Y').findFirst().orElseThrow();
        assertEquals(List.of(
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.TRI_H_TR,
                CellShape.NOTCH_DOWN,
                CellShape.TRI_H_TL,
                CellShape.EMPTY, CellShape.BLOCK, CellShape.EMPTY,
                CellShape.EMPTY, CellShape.BLOCK, CellShape.EMPTY), glyph.cells());
    }

    @Test
    void vUsesSolidUpperStemsAndTwoCellTrianglesInTheBottomRows() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'V').findFirst().orElseThrow();
        assertEquals(List.of(
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.TRI_2V_TOP_R,
                CellShape.NOTCH_DOWN,
                CellShape.TRI_2V_TOP_L,
                CellShape.TRI_2V_BOT_R, CellShape.BLOCK, CellShape.TRI_2V_BOT_L), glyph.cells());
    }

    @Test
    void wUsesRoundedOuterColumnsAndOpposingCenterPatterns() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'W').findFirst().orElseThrow();
        assertEquals(List.of(
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.BLOCK, CellShape.TRI_Q_UP, CellShape.BLOCK,
                CellShape.BLOCK,
                CellShape.NOTCH_UP,
                CellShape.BLOCK,
                CellShape.ARC_TR, CellShape.EMPTY, CellShape.ARC_TL), glyph.cells());
    }

    @Test
    void rUsesALeftwardNotchAndBlankCell11() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'R').findFirst().orElseThrow();
        assertEquals(List.of(
                CellShape.BLOCK, CellShape.BLOCK, CellShape.ARC_BL,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.BLOCK, CellShape.BLOCK,
                CellShape.NOTCH_LEFT,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK), glyph.cells());
    }

    @Test
    void eightUsesNotchesAtBothSidesOfTheWaist() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == '8').findFirst().orElseThrow();
        assertEquals(List.of(
                CellShape.ARC_BR, CellShape.BLOCK, CellShape.ARC_BL,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.NOTCH_RIGHT,
                CellShape.BLOCK,
                CellShape.NOTCH_LEFT,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.ARC_TR, CellShape.BLOCK, CellShape.ARC_TL), glyph.cells());
    }

    @Test
    void sevenUsesTheTopBarDiagonalAndLowerLeftStem() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == '7').findFirst().orElseThrow();
        for (int cell = 1; cell <= 15; cell++) {
            CellShape expected = switch (cell) {
                case 1, 2, 3, 10, 13 -> CellShape.BLOCK;
                case 5, 7 -> CellShape.TRI_H_BR;
                case 6, 8 -> CellShape.TRI_H_TL;
                default -> CellShape.EMPTY;
            };
            assertEquals(expected, glyph.cells().get(cell - 1), "Cell " + cell);
        }
    }

    @Test
    void threeUsesRoundedCornersAndALeftwardNotchInCell9() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == '3').findFirst().orElseThrow();
        assertEquals(List.of(
                CellShape.ARC_BR, CellShape.BLOCK, CellShape.ARC_BL,
                CellShape.EMPTY, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.EMPTY, CellShape.BLOCK,
                CellShape.NOTCH_LEFT,
                CellShape.EMPTY, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.ARC_TR, CellShape.BLOCK, CellShape.ARC_TL), glyph.cells());
    }

    @Test
    void bUsesALeftwardNotchInCell9() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'B').findFirst().orElseThrow();
        assertEquals(CellShape.NOTCH_LEFT, glyph.cells().get(8));
    }

    @ParameterizedTest
    @CsvSource({"E", "F"})
    void eAndFHaveShortenedMiddleBars(char character) {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == character).findFirst().orElseThrow();
        assertEquals(CellShape.BLOCK, glyph.cells().get(6));
        assertEquals(CellShape.BLOCK, glyph.cells().get(7));
        assertEquals(CellShape.EMPTY, glyph.cells().get(8));
    }

    @Test
    void slashUsesTheRequestedNumberedCells() {
        GlyphDefinition slash = GlyphLibrary.glyphs().stream()
                .filter(glyph -> glyph.character() == '/').findFirst().orElseThrow();
        for (int cell = 1; cell <= 15; cell++) {
            CellShape expected = switch (cell) {
                case 6, 8, 10 -> CellShape.TRI_H_BR;
                case 9, 11, 13 -> CellShape.TRI_H_TL;
                default -> CellShape.EMPTY;
            };
            assertEquals(expected, slash.cells().get(cell - 1), "Cell " + cell);
        }
    }

    @Test
    void jRoundsTheOutsideBottomCorners() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'J').findFirst().orElseThrow();
        assertEquals(List.of(
                CellShape.EMPTY, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.EMPTY, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.EMPTY, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.BLOCK, CellShape.EMPTY, CellShape.BLOCK,
                CellShape.ARC_TR, CellShape.BLOCK, CellShape.ARC_TL), glyph.cells());
    }

    @Test
    void mUsesSolidOuterColumnsAndTheCombinedCenter() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'M').findFirst().orElseThrow();
        for (int cell = 1; cell <= 15; cell++) {
            CellPattern expected = switch (cell) {
                case 5 -> CellShape.NOTCH_DOWN;
                case 8 -> CellShape.TRI_Q_DOWN;
                case 2, 11, 14 -> CellShape.EMPTY;
                default -> CellShape.BLOCK;
            };
            assertEquals(expected, glyph.cells().get(cell - 1), "Cell " + cell);
        }
    }

    @Test
    void nUsesSolidOuterColumnsAndTheRequestedDiagonal() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'N').findFirst().orElseThrow();
        for (int cell = 1; cell <= 15; cell++) {
            CellShape expected = switch (cell) {
                case 2, 14 -> CellShape.EMPTY;
                case 5 -> CellShape.TRI_H_BL;
                case 11 -> CellShape.TRI_H_TR;
                default -> CellShape.BLOCK;
            };
            assertEquals(expected, glyph.cells().get(cell - 1), "Cell " + cell);
        }
    }

    @Test
    void addedIUsesTheCenteredStroke() {
        GlyphDefinition glyph = GlyphLibrary.glyphs().stream()
                .filter(candidate -> candidate.character() == 'I').findFirst().orElseThrow();
        for (int i = 0; i < 15; i++) {
            assertEquals(i % 3 == 1 ? CellShape.BLOCK : CellShape.EMPTY, glyph.cells().get(i));
        }
    }

    @Test
    void spaceIsBlankAndPeriodUsesTheBottomCenterCell() {
        GlyphDefinition space = GlyphLibrary.glyphs().stream()
                .filter(glyph -> glyph.character() == ' ').findFirst().orElseThrow();
        GlyphDefinition period = GlyphLibrary.glyphs().stream()
                .filter(glyph -> glyph.character() == '.').findFirst().orElseThrow();
        assertEquals("U0020.svg", space.filename());
        assertEquals("U002E.svg", period.filename());
        for (int i = 0; i < 15; i++) {
            assertEquals(CellShape.EMPTY, space.cells().get(i));
            assertEquals(i == 13 ? CellShape.BLOCK : CellShape.EMPTY, period.cells().get(i));
        }
    }
}
