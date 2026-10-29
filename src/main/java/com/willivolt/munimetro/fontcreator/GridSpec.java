package com.willivolt.munimetro.fontcreator;

public record GridSpec(double cellSize, double gap) {
    public static final int COLUMNS = 3;
    public static final int ROWS = 5;
    public static final int CELL_COUNT = COLUMNS * ROWS;
    public static final GridSpec DEFAULT = new GridSpec(140, 10);

    public GridSpec {
        if (!Double.isFinite(cellSize) || cellSize <= 0
                || !Double.isFinite(gap) || gap < 0
                || !Double.isFinite(COLUMNS * cellSize + (COLUMNS - 1) * gap)
                || !Double.isFinite(ROWS * cellSize + (ROWS - 1) * gap)) {
            throw new IllegalArgumentException("Cell size must be positive and gap nonnegative, with finite canvas dimensions.");
        }
    }

    public double pitch() {
        return cellSize + gap;
    }

    public double width() {
        return COLUMNS * cellSize + (COLUMNS - 1) * gap;
    }

    public double height() {
        return ROWS * cellSize + (ROWS - 1) * gap;
    }

    public double x(int index) {
        checkIndex(index);
        return (index % COLUMNS) * pitch();
    }

    public double y(int index) {
        checkIndex(index);
        return (index / COLUMNS) * pitch();
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= CELL_COUNT) {
            throw new IndexOutOfBoundsException("Cell index must be between 0 and " + (CELL_COUNT - 1) + ": " + index);
        }
    }
}
