package com.willivolt.munimetro.fontcreator;

import static com.willivolt.munimetro.fontcreator.CellPattern.combine;
import static com.willivolt.munimetro.fontcreator.CellShape.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CellPatternTest {
    @Test
    void combinationDrawsEachPrimitiveAtTheSameLocalOrigin() throws Exception {
        CellPattern pattern = combine(TRI_Q_UP, TRI_Q_LEFT, TRI_Q_RIGHT);
        assertEquals("TRI_Q_UP+TRI_Q_LEFT+TRI_Q_RIGHT", pattern.name());
        assertEquals(TRI_Q_UP.svg(GridSpec.DEFAULT) + TRI_Q_LEFT.svg(GridSpec.DEFAULT)
                + TRI_Q_RIGHT.svg(GridSpec.DEFAULT), pattern.svg(GridSpec.DEFAULT));
        var svg = SvgTestSupport.parse("<svg xmlns=\"http://www.w3.org/2000/svg\">"
                + pattern.svg(GridSpec.DEFAULT) + "</svg>");
        assertEquals(3, svg.getElementsByTagName("polygon").getLength());
        assertEquals(0, svg.getElementsByTagName("g").getLength());
    }

    @Test
    void threeQuarterTrianglesFillExactlyTheComplementOfTheDownTriangle() throws Exception {
        for (GridSpec grid : new GridSpec[]{GridSpec.DEFAULT, new GridSpec(4, 1), new GridSpec(140.25, 10.5)}) {
            Area combined = new Area();
            for (CellShape shape : List.of(TRI_Q_UP, TRI_Q_LEFT, TRI_Q_RIGHT)) {
                combined.add(area(shape, grid));
            }
            Area expected = new Area(new Rectangle2D.Double(0, 0, grid.cellSize(), grid.cellSize()));
            expected.subtract(area(TRI_Q_DOWN, grid));
            expected.exclusiveOr(combined);
            assertTrue(expected.isEmpty(), "Combination must equal the cell minus TRI_Q_DOWN.");
        }
    }

    @Test
    void combinationSupportsAnyPrimitiveIncludingEmptyAndOverlaps() {
        GridSpec grid = GridSpec.DEFAULT;
        assertEquals(ARC_TR.svg(grid) + TRI_H_BL.svg(grid),
                combine(EMPTY, ARC_TR, TRI_H_BL).svg(grid));
        assertEquals(BLOCK.svg(grid) + BLOCK.svg(grid), combine(BLOCK, BLOCK).svg(grid));
        assertEquals("", combine(EMPTY, EMPTY).svg(grid));
        assertEquals(BLOCK.svg(grid), combine(BLOCK).svg(grid));
    }

    @Test
    void combinationCopiesInputArraysAndLists() {
        CellShape[] shapes = {TRI_Q_UP, TRI_Q_LEFT, TRI_Q_RIGHT};
        CellPattern pattern = combine(shapes);
        shapes[0] = EMPTY;
        assertEquals(combine(TRI_Q_UP, TRI_Q_LEFT, TRI_Q_RIGHT), pattern);
        List<CellShape> list = new ArrayList<>(List.of(BLOCK));
        CellPattern.Combined combined = new CellPattern.Combined(list);
        list.set(0, EMPTY);
        assertEquals(List.of(BLOCK), combined.shapes());
        assertThrows(UnsupportedOperationException.class, () -> combined.shapes().set(0, EMPTY));
    }

    @Test
    void rejectsEmptyAndNullCombinations() {
        assertThrows(IllegalArgumentException.class, () -> combine());
        assertThrows(NullPointerException.class, () -> combine((CellShape[]) null));
        assertThrows(NullPointerException.class, () -> combine(BLOCK, null));
        assertThrows(NullPointerException.class, () -> new CellPattern.Combined(null));
        assertThrows(IllegalArgumentException.class, () -> new CellPattern.Combined(List.of()));
    }

    private static Area area(CellShape shape, GridSpec grid) throws Exception {
        double[][] points = SvgTestSupport.points(shape, grid);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(points[0][0], points[0][1]);
        for (int i = 1; i < points.length; i++) {
            path.lineTo(points[i][0], points[i][1]);
        }
        path.closePath();
        return new Area(path);
    }
}
