package com.willivolt.munimetro.fontcreator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GridSpecTest {
    @Test
    void defaultGridUsesFiveRowsOfThreeSquareCells() {
        GridSpec grid = GridSpec.DEFAULT;
        assertEquals(140, grid.cellSize());
        assertEquals(10, grid.gap());
        assertEquals(150, grid.pitch());
        assertEquals(440, grid.width());
        assertEquals(740, grid.height());
        for (int i = 0; i < 15; i++) {
            assertEquals((i % 3) * 150, grid.x(i));
            assertEquals((i / 3) * 150, grid.y(i));
        }
    }

    @Test
    void supportsOtherSizesAndZeroGap() {
        GridSpec grid = new GridSpec(90, 10);
        assertEquals(290, grid.width());
        assertEquals(490, grid.height());
        assertEquals(450, new GridSpec(90, 0).height());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE})
    void rejectsInvalidCellSizes(double size) {
        assertThrows(IllegalArgumentException.class, () -> new GridSpec(size, 10));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE})
    void rejectsInvalidGaps(double gap) {
        assertThrows(IllegalArgumentException.class, () -> new GridSpec(140, gap));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 15})
    void rejectsIndicesOutsideTheGrid(int index) {
        assertThrows(IndexOutOfBoundsException.class, () -> GridSpec.DEFAULT.x(index));
        assertThrows(IndexOutOfBoundsException.class, () -> GridSpec.DEFAULT.y(index));
    }
}
