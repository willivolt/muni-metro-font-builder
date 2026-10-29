package com.willivolt.munimetro.fontcreator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class SvgGlyphWriter {
    private final GridSpec grid;

    public SvgGlyphWriter(GridSpec grid) {
        this.grid = Objects.requireNonNull(grid, "Grid must not be null.");
    }

    public String render(GlyphDefinition glyph) {
        StringBuilder svg = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(grid.width()).append("\" height=\"").append(grid.height())
                .append("\" viewBox=\"0 0 ").append(grid.width()).append(' ')
                .append(grid.height()).append("\">\n")
                .append("  <g fill=\"black\" stroke=\"none\">\n");
        for (int i = 0; i < GridSpec.CELL_COUNT; i++) {
            CellPattern shape = glyph.cells().get(i);
            String geometry = shape.svg(grid);
            if (!geometry.isEmpty()) {
                svg.append("    <g transform=\"translate(").append(grid.x(i)).append(' ')
                        .append(grid.y(i)).append(")\" data-cell=\"").append(i)
                        .append("\" data-shape=\"").append(shape.name()).append("\">")
                        .append(geometry).append("</g>\n");
            }
        }
        return svg.append("  </g>\n</svg>\n").toString();
    }

    public Path write(Path directory, GlyphDefinition glyph) throws IOException {
        Files.createDirectories(directory);
        return Files.writeString(directory.resolve(glyph.filename()), render(glyph), StandardCharsets.UTF_8);
    }
}
