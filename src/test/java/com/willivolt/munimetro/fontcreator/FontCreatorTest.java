package com.willivolt.munimetro.fontcreator;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class FontCreatorTest {
    @TempDir
    Path temporary;

    @Test
    void generatesAllGlyphsAndCanRegenerateThem() throws Exception {
        Path directory = temporary.resolve("glyphs");
        assertEquals(37, FontCreator.generate(directory));
        var expected = GlyphLibrary.glyphs().stream().map(GlyphDefinition::filename).collect(Collectors.toSet());
        try (var files = Files.list(directory)) {
            assertEquals(expected, files.map(path -> path.getFileName().toString()).collect(Collectors.toSet()));
        }
        String original = Files.readString(directory.resolve("U0041.svg"));
        Files.writeString(directory.resolve("U0041.svg"), "old");
        Path unrelated = Files.writeString(directory.resolve("notes.txt"), "keep");
        assertEquals(37, FontCreator.generate(directory));
        assertEquals(original, Files.readString(directory.resolve("U0041.svg")));
        assertEquals("keep", Files.readString(unrelated));
    }

    @Test
    void mainAcceptsADirectoryContainingSpacesAndReportsSuccess() throws Exception {
        Path directory = temporary.resolve("review glyphs");
        Result result = invoke(directory.toString());
        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("Generated 37 SVG glyphs in " + directory.toAbsolutePath().normalize()));
        assertTrue(Files.exists(directory.resolve("U003A.svg")));
        assertTrue(Files.exists(directory.resolve("U0020.svg")));
        assertTrue(Files.exists(directory.resolve("U002E.svg")));
    }

    @Test
    void mainRejectsExcessArgumentsWithNonzeroExit() throws Exception {
        Result result = invoke(temporary.resolve("unused").toString(), "extra");
        assertEquals(1, result.exitCode());
        assertTrue(result.output().contains("Usage:"));
        assertFalse(result.output().contains("Generated"));
        assertFalse(Files.exists(temporary.resolve("unused")));
    }

    @Test
    void mainReportsFilesystemFailureWithNonzeroExit() throws Exception {
        Path file = Files.writeString(temporary.resolve("file.txt"), "preserve");
        Result result = invoke(file.toString());
        assertEquals(1, result.exitCode());
        assertTrue(result.output().contains("SVG generation failed:"));
        assertFalse(result.output().contains("Generated"));
        assertEquals("preserve", Files.readString(file));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void mainReportsInvalidPathsWithNonzeroExit() throws Exception {
        Result result = invoke("invalid|path");
        assertEquals(1, result.exitCode());
        assertTrue(result.output().contains("SVG generation failed:"));
    }

    private Result invoke(String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), FontCreator.class.getName()));
        command.addAll(List.of(args));
        Path log = temporary.resolve("process.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "CLI process did not finish.");
            return new Result(process.exitValue(), Files.readString(log, StandardCharsets.UTF_8));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private record Result(int exitCode, String output) {
    }
}
