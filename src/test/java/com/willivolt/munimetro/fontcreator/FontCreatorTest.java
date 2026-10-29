package com.willivolt.munimetro.fontcreator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FontCreatorTest {
    @Test
    void mainPrintsGreeting() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            FontCreator.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals("Hello World!" + System.lineSeparator(), output.toString(StandardCharsets.UTF_8));
    }
}
