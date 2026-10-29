package com.willivolt.munimetro.fontcreator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;

class CellShapeTest {
    @ParameterizedTest
    @EnumSource(CellShape.class)
    void everyShapeProducesValidSvg(CellShape shape) throws Exception {
        Element svg = SvgTestSupport.shape(shape, GridSpec.DEFAULT);
        assertEquals(shape == CellShape.EMPTY ? 0 : 1, svg.getChildNodes().getLength());
        assertEquals(18, CellShape.values().length);
        if (shape == CellShape.BLOCK) {
            Element rect = (Element) svg.getFirstChild();
            assertEquals("140.0", rect.getAttribute("width"));
            assertEquals("140.0", rect.getAttribute("height"));
        }
    }

    @ParameterizedTest
    @MethodSource("arcPaths")
    void quarterCirclesUseTheNamedCornerAsCenter(CellShape shape, String expected) throws Exception {
        Element path = (Element) SvgTestSupport.shape(shape, GridSpec.DEFAULT).getFirstChild();
        assertEquals(expected, path.getAttribute("d"));
    }

    static Stream<Arguments> arcPaths() {
        return Stream.of(
                Arguments.of(CellShape.ARC_TL, "M 0.0,0.0 L 140.0,0.0 A 140.0,140.0 0 0 1 0.0,140.0 Z"),
                Arguments.of(CellShape.ARC_TR, "M 140.0,0.0 L 140.0,140.0 A 140.0,140.0 0 0 1 0.0,0.0 Z"),
                Arguments.of(CellShape.ARC_BL, "M 0.0,140.0 L 0.0,0.0 A 140.0,140.0 0 0 1 140.0,140.0 Z"),
                Arguments.of(CellShape.ARC_BR, "M 140.0,140.0 L 0.0,140.0 A 140.0,140.0 0 0 1 140.0,0.0 Z"));
    }

    @ParameterizedTest
    @MethodSource("trianglePoints")
    void trianglesUseTheDocumentedFilledCornerOrPointDirection(CellShape shape, String expected) throws Exception {
        Element polygon = (Element) SvgTestSupport.shape(shape, GridSpec.DEFAULT).getFirstChild();
        assertEquals(expected, polygon.getAttribute("points"));
    }

    static Stream<Arguments> trianglePoints() {
        return Stream.of(
                Arguments.of(CellShape.TRI_H_TL, "0.0,0.0 140.0,0.0 0.0,140.0"),
                Arguments.of(CellShape.TRI_H_TR, "0.0,0.0 140.0,0.0 140.0,140.0"),
                Arguments.of(CellShape.TRI_H_BL, "0.0,0.0 0.0,140.0 140.0,140.0"),
                Arguments.of(CellShape.TRI_H_BR, "140.0,0.0 140.0,140.0 0.0,140.0"),
                Arguments.of(CellShape.TRI_Q_UP, "0.0,140.0 70.0,70.0 140.0,140.0"),
                Arguments.of(CellShape.TRI_Q_DOWN, "0.0,0.0 140.0,0.0 70.0,70.0"),
                Arguments.of(CellShape.TRI_Q_LEFT, "140.0,0.0 70.0,70.0 140.0,140.0"),
                Arguments.of(CellShape.TRI_Q_RIGHT, "0.0,0.0 70.0,70.0 0.0,140.0"));
    }

    @ParameterizedTest
    @EnumSource(value = CellShape.class, names = {"EMPTY", "BLOCK", "ARC_TL", "ARC_TR", "ARC_BL", "ARC_BR"},
            mode = EnumSource.Mode.EXCLUDE)
    void polygonCoordinatesStayInsideTheirCell(CellShape shape) throws Exception {
        for (GridSpec grid : new GridSpec[]{GridSpec.DEFAULT, new GridSpec(90, 0), new GridSpec(4, 1)}) {
            for (double[] point : SvgTestSupport.points(shape, grid)) {
                for (double coordinate : point) {
                    assertTrue(Double.isFinite(coordinate) && coordinate >= 0 && coordinate <= grid.cellSize());
                }
            }
        }
    }

    @Test
    void twoCellFragmentsFollowOneDiagonalAcrossTheGap() throws Exception {
        for (GridSpec grid : new GridSpec[]{GridSpec.DEFAULT, new GridSpec(90, 10), new GridSpec(4, 1), new GridSpec(90, 0)}) {
            double s = grid.cellSize();
            double height = 2 * s + grid.gap();
            double[][] topLeft = SvgTestSupport.points(CellShape.TRI_2V_TOP_L, grid);
            double[][] bottomLeft = SvgTestSupport.points(CellShape.TRI_2V_BOT_L, grid);
            double[][] topRight = SvgTestSupport.points(CellShape.TRI_2V_TOP_R, grid);
            double[][] bottomRight = SvgTestSupport.points(CellShape.TRI_2V_BOT_R, grid);
            assertEquals(s * (1 - s / height), topLeft[2][0], 1e-12);
            assertEquals(s * (1 - grid.pitch() / height), bottomLeft[1][0], 1e-12);
            assertEquals(s * s / height, topRight[3][0], 1e-12);
            assertEquals(s * grid.pitch() / height, bottomRight[0][0], 1e-12);
            assertEquals(-s / height, (bottomLeft[1][0] - s) / grid.pitch(), 1e-12);
            assertEquals(-s / height, (topLeft[2][0] - s) / s, 1e-12);
            assertMirrored(topLeft, topRight, s);
            assertMirrored(bottomLeft, bottomRight, s);
        }
    }

    @Test
    void preservesSubdecimalIntersectionPrecision() throws Exception {
        double width = SvgTestSupport.points(CellShape.TRI_2V_BOT_L, GridSpec.DEFAULT)[1][0];
        assertEquals(1960.0 / 29.0, width, 1e-12);
        assertNotEquals(Math.round(width * 10) / 10.0, width);
    }

    private static void assertMirrored(double[][] left, double[][] right, double size) {
        assertEquals(left.length, right.length);
        for (double[] point : left) {
            assertTrue(Arrays.stream(right).anyMatch(other ->
                    Math.abs(size - point[0] - other[0]) < 1e-12 && point[1] == other[1]));
        }
    }
}
