package com.willivolt.munimetro.fontcreator;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

final class SvgTestSupport {
    private SvgTestSupport() {
    }

    static Element parse(String svg) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg))).getDocumentElement();
    }

    static Element shape(CellShape shape, GridSpec grid) throws Exception {
        return parse("<svg xmlns=\"http://www.w3.org/2000/svg\">" + shape.svg(grid) + "</svg>");
    }

    static double[][] points(CellShape shape, GridSpec grid) throws Exception {
        Element polygon = (Element) shape(shape, grid).getElementsByTagName("polygon").item(0);
        String[] pairs = polygon.getAttribute("points").split(" ");
        double[][] points = new double[pairs.length][2];
        for (int i = 0; i < pairs.length; i++) {
            String[] coordinates = pairs[i].split(",");
            points[i][0] = Double.parseDouble(coordinates[0]);
            points[i][1] = Double.parseDouble(coordinates[1]);
        }
        return points;
    }
}
