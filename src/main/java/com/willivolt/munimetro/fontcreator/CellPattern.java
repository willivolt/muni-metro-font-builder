package com.willivolt.munimetro.fontcreator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public sealed interface CellPattern permits CellShape, CellPattern.Combined {
    String svg(GridSpec grid);

    String name();

    static CellPattern combine(CellShape... shapes) {
        return new Combined(Arrays.asList(Objects.requireNonNull(shapes, "Shapes must not be null.")));
    }

    record Combined(List<CellShape> shapes) implements CellPattern {
        public Combined {
            shapes = List.copyOf(shapes);
            if (shapes.isEmpty()) {
                throw new IllegalArgumentException("A combination must contain at least one shape; use EMPTY for a blank cell.");
            }
        }

        @Override
        public String svg(GridSpec grid) {
            return shapes.stream().map(shape -> shape.svg(grid)).collect(Collectors.joining());
        }

        @Override
        public String name() {
            return shapes.stream().map(CellShape::name).collect(Collectors.joining("+"));
        }
    }
}
