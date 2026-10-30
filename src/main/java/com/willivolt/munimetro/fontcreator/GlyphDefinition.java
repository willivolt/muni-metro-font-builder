package com.willivolt.munimetro.fontcreator;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record GlyphDefinition(char character, List<CellPattern> cells) {
    public GlyphDefinition {
        if (Character.isSurrogate(character)) {
            throw new IllegalArgumentException("A glyph character must not be a surrogate.");
        }
        Objects.requireNonNull(cells, "Glyph cells must not be null.");
        if (cells.size() != GridSpec.CELL_COUNT) {
            throw new IllegalArgumentException("Glyph must contain exactly " + GridSpec.CELL_COUNT + " cells.");
        }
        cells = List.copyOf(cells);
    }

    public GlyphDefinition(char character, CellPattern... cells) {
        this(character, Arrays.asList(Objects.requireNonNull(cells, "Glyph cells must not be null.")));
    }

    public String filename() {
        return String.format(Locale.ROOT, "U%04X.svg", (int) character);
    }
}
