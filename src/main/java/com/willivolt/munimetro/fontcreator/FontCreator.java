package com.willivolt.munimetro.fontcreator;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class FontCreator {
    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -jar muni-metro-font-builder-1.0-SNAPSHOT.jar [output-directory]");
            System.exit(1);
            return;
        }
        try {
            Path directory = args.length == 0 ? Path.of("target", "glyphs") : Path.of(args[0]);
            int count = generate(directory);
            System.out.println("Generated " + count + " SVG glyphs in " + directory.toAbsolutePath().normalize());
        } catch (IOException | InvalidPathException e) {
            System.err.println("SVG generation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public static int generate(Path directory) throws IOException {
        SvgGlyphWriter writer = new SvgGlyphWriter(GridSpec.DEFAULT);
        var glyphs = GlyphLibrary.glyphs();
        for (GlyphDefinition glyph : glyphs) {
            writer.write(directory, glyph);
        }
        return glyphs.size();
    }
}
