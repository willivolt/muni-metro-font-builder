package com.willivolt.munimetro.fontcreator;

public enum CellShape implements CellPattern {
    EMPTY, BLOCK,
    ARC_TL, ARC_TR, ARC_BL, ARC_BR,
    TRI_H_TL, TRI_H_TR, TRI_H_BL, TRI_H_BR,
    TRI_Q_UP, TRI_Q_DOWN, TRI_Q_LEFT, TRI_Q_RIGHT,
    TRI_2V_TOP_L, TRI_2V_TOP_R, TRI_2V_BOT_L, TRI_2V_BOT_R;

    public String svg(GridSpec grid) {
        double s = grid.cellSize();
        double mid = s / 2;
        // Both fragments follow the same diagonal across two cells and the intervening gap.
        double bottomWidth = s * (s / (2 * s + grid.gap()));
        double topWidth = s - bottomWidth;

        return switch (this) {
            case EMPTY -> "";
            case BLOCK -> "<rect width=\"" + s + "\" height=\"" + s + "\" />";
            case ARC_TL -> arc(s, 0, 0, s, 0, 0, s);
            case ARC_TR -> arc(s, s, 0, s, s, 0, 0);
            case ARC_BL -> arc(s, 0, s, 0, 0, s, s);
            case ARC_BR -> arc(s, s, s, 0, s, s, 0);
            case TRI_H_TL -> polygon(0, 0, s, 0, 0, s);
            case TRI_H_TR -> polygon(0, 0, s, 0, s, s);
            case TRI_H_BL -> polygon(0, 0, 0, s, s, s);
            case TRI_H_BR -> polygon(s, 0, s, s, 0, s);
            case TRI_Q_UP -> polygon(0, s, mid, mid, s, s);
            case TRI_Q_DOWN -> polygon(0, 0, s, 0, mid, mid);
            case TRI_Q_LEFT -> polygon(s, 0, mid, mid, s, s);
            case TRI_Q_RIGHT -> polygon(0, 0, mid, mid, 0, s);
            case TRI_2V_TOP_L -> polygon(0, 0, s, 0, topWidth, s, 0, s);
            case TRI_2V_TOP_R -> polygon(0, 0, s, 0, s, s, bottomWidth, s);
            case TRI_2V_BOT_L -> polygon(0, 0, bottomWidth, 0, 0, s);
            case TRI_2V_BOT_R -> polygon(topWidth, 0, s, 0, s, s);
        };
    }

    private static String arc(double radius, double cx, double cy,
                              double startX, double startY, double endX, double endY) {
        return "<path d=\"M " + cx + "," + cy
                + " L " + startX + "," + startY
                + " A " + radius + "," + radius + " 0 0 1 " + endX + "," + endY + " Z\" />";
    }

    private static String polygon(double... coordinates) {
        StringBuilder points = new StringBuilder("<polygon points=\"");
        for (int i = 0; i < coordinates.length; i += 2) {
            if (i > 0) {
                points.append(' ');
            }
            points.append(coordinates[i]).append(',').append(coordinates[i + 1]);
        }
        return points.append("\" />").toString();
    }
}
