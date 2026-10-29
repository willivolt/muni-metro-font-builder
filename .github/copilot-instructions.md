# Repository instructions

## Build and test

Use JDK 21 and Maven 3.9 or later. Run commands from the repository root:

```powershell
mvn verify
mvn test
mvn "-Dtest=FontCreatorTest" test
mvn "-Dtest=FontCreatorTest#mainPrintsGreeting" test
java -jar target\muni-metro-font-builder-1.0-SNAPSHOT.jar
```

`mvn verify` runs tests and packages the executable JAR. Use `mvn clean verify`
after renaming classes to remove stale compiled classes and test reports.

## Architecture

This is a single-module Maven Java application for creating a font inspired by
MUNI Metro Breda train signs. It currently contains only a greeting entry point
and its test; font-generation behavior has not been implemented.

The application entry point is
`com.willivolt.munimetro.fontcreator.FontCreator`. The Maven JAR plugin references
that fully qualified name in the manifest, allowing the packaged application to
run with `java -jar`. Keep the manifest synchronized with entry-point renames.

Production and test code use the `com.willivolt.munimetro.fontcreator` base
package under Maven's standard source roots. JUnit Jupiter is a test-scoped
dependency; the application currently has no runtime dependencies.

## Conventions

- Use `FontCreator` and `FontCreatorTest` for the entry point and its test,
  rather than the archetype names `App` and `AppTest`.
- Compiler release and source encoding are configured in `pom.xml` as Java 21
  and UTF-8.
- The entry-point test captures standard output with an explicit UTF-8 charset,
  restores `System.out` in a `finally` block, and accounts for the platform line
  separator. Preserve those safeguards when changing output assertions.
- Maven build output belongs in the ignored `target` directory.
