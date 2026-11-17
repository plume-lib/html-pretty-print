package org.plumelib.htmlprettyprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * End-to-end tests for the HtmlPrettyPrint program. Each test runs the program as a separate
 * process, in a fresh working directory, and compares everything the program produced -- its exit
 * status, its standard output, its standard error, and the files it wrote -- to goal files stored
 * in the repository.
 *
 * <p>The test data is in {@code src/test/resources/end-to-end}, one subdirectory per test case; see
 * {@code src/test/resources/end-to-end/README.txt} for the layout of a test case. Every
 * subdirectory is a test case, so a new test case can be added without changing this file.
 *
 * <p>By default, the program under test is this project's HtmlPrettyPrint class, run in a new JVM.
 * If the system property {@code e2e.command} is set (for example, by {@code ./gradlew test
 * -Pe2e.command="python3 /path/to/htmlpp.py"}), its value, split at whitespace, is the command that
 * runs the program instead; the test case's arguments follow it.
 */
public final class HtmlPrettyPrintEndToEndTest {

  /** Creates a new HtmlPrettyPrintEndToEndTest. */
  public HtmlPrettyPrintEndToEndTest() {}

  /** The classpath resource that contains one subdirectory per test case. */
  private static final String testDataResource = "/end-to-end";

  /** The directory that contains one subdirectory per test case. */
  private static final Path testDataDir = findTestDataDir();

  /** The file names that are permitted to appear in a test case directory. */
  private static final Set<String> permittedCaseFiles =
      Set.of(
          "description.txt",
          "input",
          "args.txt",
          "stdin.txt",
          "goal",
          "goal-out.txt",
          "goal-err.txt",
          "goal-err-prefix.txt",
          "goal-status.txt");

  /**
   * The text that replaces the working directory's name in the program's standard output and
   * standard error, so that the goal files do not depend on where the working directory is.
   */
  private static final String workingDirPlaceholder = "{WORKDIR}";

  /** How long to wait for the program to terminate, in seconds. */
  private static final int timeoutSeconds = 120;

  /**
   * Returns the directory that contains the test cases.
   *
   * @return the directory that contains the test cases
   */
  private static Path findTestDataDir() {
    URL url = HtmlPrettyPrintEndToEndTest.class.getResource(testDataResource);
    if (url == null) {
      throw new Error("Cannot find resource " + testDataResource + " on the classpath.");
    }
    try {
      return Path.of(url.toURI());
    } catch (URISyntaxException e) {
      throw new Error("Cannot convert " + url + " to a path.", e);
    }
  }

  /**
   * Returns the name of each test case: that is, of each subdirectory of the test data directory.
   *
   * @return the name of each test case
   * @throws IOException if the test data directory cannot be read
   */
  private static List<String> testCaseNames() throws IOException {
    List<String> result = new ArrayList<>();
    try (Stream<Path> children = Files.list(testDataDir)) {
      for (Path child : children.toList()) {
        if (Files.isDirectory(child)) {
          result.add(child.toFile().getName());
        }
      }
    }
    result.sort(Comparator.naturalOrder());
    assertTrue(!result.isEmpty(), "No test cases in " + testDataDir);
    return result;
  }

  /**
   * Runs one end-to-end test case.
   *
   * @param caseName the name of the test case, which is the name of a subdirectory of the test data
   *     directory
   * @param workingDir the directory in which to run the program
   * @param captureDir a directory in which to capture the program's standard output and standard
   *     error; it is separate from {@code workingDir} so that the capture files are not mistaken
   *     for files that the program wrote
   * @throws Exception if the test case cannot be read or the program cannot be run
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("testCaseNames")
  public void endToEnd(String caseName, @TempDir Path workingDir, @TempDir Path captureDir)
      throws Exception {
    Path caseDir = testDataDir.resolve(caseName);
    checkCaseDir(caseDir);

    Path inputDir = caseDir.resolve("input");
    if (Files.isDirectory(inputDir)) {
      copyTree(inputDir, workingDir.resolve("input"));
    }

    List<String> args = arguments(caseDir, workingDir);
    Path stdinFile = caseDir.resolve("stdin.txt");
    if (!Files.isRegularFile(stdinFile)) {
      stdinFile = captureDir.resolve("empty-stdin.txt");
      Files.write(stdinFile, new byte[0]);
    }
    Path stdoutFile = captureDir.resolve("stdout.txt");
    Path stderrFile = captureDir.resolve("stderr.txt");
    int status = runProgram(workingDir, args, stdinFile, stdoutFile, stderrFile);

    String context = "Test case " + caseName + ", arguments " + args + ".";

    assertEquals(goalStatus(caseDir), status, context + "  Wrong exit status.");
    assertEquals(
        goalText(caseDir.resolve("goal-out.txt")),
        normalizeOutput(readText(stdoutFile), workingDir),
        context + "  Wrong standard output.");
    checkStderr(caseDir, normalizeOutput(readText(stderrFile), workingDir), context);
    checkWorkingDir(caseDir, workingDir, context);
  }

  /**
   * Fails if the test case directory contains a file whose name the test harness does not know
   * about; such a file is probably a typo in a goal file's name, which would otherwise be silently
   * ignored.
   *
   * @param caseDir the test case directory
   * @throws IOException if the directory cannot be read
   */
  private static void checkCaseDir(Path caseDir) throws IOException {
    try (Stream<Path> children = Files.list(caseDir)) {
      for (Path child : children.toList()) {
        String name = child.toFile().getName();
        if (!permittedCaseFiles.contains(name)) {
          fail(
              "Test case "
                  + caseDir.toFile().getName()
                  + " contains unrecognized file "
                  + name
                  + ".  Permitted files are "
                  + new TreeSet<>(permittedCaseFiles)
                  + ".");
        }
      }
    }
  }

  /**
   * Returns the command-line arguments for the test case: the lines of the case's {@code args.txt},
   * or, if the case has no {@code args.txt}, every file in the case's {@code input} directory.
   *
   * @param caseDir the test case directory
   * @param workingDir the directory in which the program will be run
   * @return the command-line arguments for the test case
   * @throws IOException if a file cannot be read
   */
  private static List<String> arguments(Path caseDir, Path workingDir) throws IOException {
    Path argsFile = caseDir.resolve("args.txt");
    if (Files.isRegularFile(argsFile)) {
      List<String> result = new ArrayList<>();
      for (String line : Files.readAllLines(argsFile, StandardCharsets.UTF_8)) {
        if (!line.isEmpty()) {
          result.add(line);
        }
      }
      return result;
    }
    Path inputDir = workingDir.resolve("input");
    if (!Files.isDirectory(inputDir)) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    // `relativeFileNames` returns a sorted map, so the arguments are in sorted order.
    for (String name : relativeFileNames(inputDir).keySet()) {
      result.add("input/" + name);
    }
    return result;
  }

  /**
   * Runs the program.
   *
   * @param workingDir the working directory for the program
   * @param args the command-line arguments for the program
   * @param stdinFile the file to supply as the program's standard input
   * @param stdoutFile where to write the program's standard output
   * @param stderrFile where to write the program's standard error
   * @return the program's exit status
   * @throws IOException if the program cannot be started
   * @throws InterruptedException if this thread is interrupted while the program runs
   */
  private static int runProgram(
      Path workingDir, List<String> args, Path stdinFile, Path stdoutFile, Path stderrFile)
      throws IOException, InterruptedException {
    List<String> command = new ArrayList<>(programCommand());
    command.addAll(args);
    ProcessBuilder processBuilder =
        new ProcessBuilder(command)
            .directory(workingDir.toFile())
            .redirectInput(stdinFile.toFile())
            .redirectOutput(stdoutFile.toFile())
            .redirectError(stderrFile.toFile());
    // Make the program's treatment of non-ASCII file names independent of the user's locale.
    processBuilder.environment().put("LC_ALL", "C.UTF-8");
    Process process = processBuilder.start();
    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new Error("The program did not terminate within " + timeoutSeconds + " seconds.");
    }
    return process.exitValue();
  }

  /**
   * Returns the command that runs the program, without the test case's arguments. It is the value
   * of the system property {@code e2e.command}, split at whitespace, if that property is set and
   * not blank; otherwise it runs this project's HtmlPrettyPrint class in a new JVM.
   *
   * @return the command that runs the program
   */
  private static List<String> programCommand() {
    String e2eCommand = System.getProperty("e2e.command");
    if (e2eCommand != null && !e2eCommand.isBlank()) {
      return Arrays.asList(e2eCommand.trim().split("\\s+"));
    }
    return List.of(javaExecutable(), "-cp", classpath(), HtmlPrettyPrint.class.getName());
  }

  /**
   * Returns the java executable of the JVM that is running the tests.
   *
   * @return the java executable of the JVM that is running the tests
   */
  private static String javaExecutable() {
    String javaHome = System.getProperty("java.home");
    if (javaHome == null) {
      return "java";
    }
    Path java = Path.of(javaHome, "bin", "java");
    if (Files.isRegularFile(java)) {
      return java.toString();
    }
    Path javaExe = Path.of(javaHome, "bin", "java.exe");
    if (Files.isRegularFile(javaExe)) {
      return javaExe.toString();
    }
    return "java";
  }

  /**
   * Returns the classpath with which to run the program, which is the classpath of the JVM that is
   * running the tests.
   *
   * @return the classpath with which to run the program
   */
  private static String classpath() {
    String result = System.getProperty("java.class.path");
    if (result == null) {
      throw new Error("Property java.class.path is not set.");
    }
    return result;
  }

  /**
   * Returns the expected exit status of the test case, which is 0 if the test case does not say.
   *
   * @param caseDir the test case directory
   * @return the expected exit status of the test case
   * @throws IOException if the goal file cannot be read
   */
  private static int goalStatus(Path caseDir) throws IOException {
    Path goalStatusFile = caseDir.resolve("goal-status.txt");
    if (!Files.isRegularFile(goalStatusFile)) {
      return 0;
    }
    return Integer.parseInt(readText(goalStatusFile).trim());
  }

  /**
   * Fails if the program's standard error is not what the test case says it should be. The test
   * case gives either the whole of the standard error (in {@code goal-err.txt}) or a prefix of it
   * (in {@code goal-err-prefix.txt}, for output such as an exception message that differs among
   * platforms and would make a whole-text goal file brittle). If the test case gives neither file,
   * the program should write nothing to standard error.
   *
   * @param caseDir the test case directory
   * @param stderr what the program wrote to standard error, after normalization
   * @param context a description of the test case, for failure messages
   * @throws IOException if a goal file cannot be read
   */
  private static void checkStderr(Path caseDir, String stderr, String context) throws IOException {
    Path prefixFile = caseDir.resolve("goal-err-prefix.txt");
    if (Files.isRegularFile(prefixFile)) {
      // Remove the goal file's final line separator, which is not part of the prefix.
      String prefix = goalText(prefixFile).stripTrailing();
      assertTrue(
          stderr.startsWith(prefix),
          context
              + "  Standard error should start with:%n%s%nbut was:%n%s".formatted(prefix, stderr));
      return;
    }
    assertEquals(
        goalText(caseDir.resolve("goal-err.txt")), stderr, context + "  Wrong standard error.");
  }

  /**
   * Fails if the files in the working directory are not the files in the test case's {@code goal}
   * directory. The {@code input} directory, which the test harness rather than the program created,
   * is ignored.
   *
   * @param caseDir the test case directory
   * @param workingDir the directory in which the program ran
   * @param context a description of the test case, for failure messages
   * @throws IOException if a file cannot be read
   */
  private static void checkWorkingDir(Path caseDir, Path workingDir, String context)
      throws IOException {
    Map<String, Path> actualFiles = relativeFileNames(workingDir);
    actualFiles.keySet().removeIf(name -> name.startsWith("input/"));

    Path goalDir = caseDir.resolve("goal");
    Map<String, Path> goalFiles =
        Files.isDirectory(goalDir) ? relativeFileNames(goalDir) : new TreeMap<>();

    assertEquals(
        String.join(System.lineSeparator(), goalFiles.keySet()),
        String.join(System.lineSeparator(), actualFiles.keySet()),
        context + "  Wrong files in the working directory.");
    for (Map.Entry<String, Path> goalFile : goalFiles.entrySet()) {
      String name = goalFile.getKey();
      Path actualFile = actualFiles.get(name);
      if (actualFile == null) {
        throw new Error("This can't happen: " + name);
      }
      assertEquals(
          normalizeLineSeparators(readText(goalFile.getValue())),
          normalizeLineSeparators(readText(actualFile)),
          context + "  Wrong contents of file " + name + ".");
    }
  }

  /**
   * Returns every file (not directory) under the given directory, as a map from the file's name
   * relative to the given directory (using "/" as the separator) to the file.
   *
   * @param dir the directory to walk
   * @return the files under the given directory
   * @throws IOException if the directory cannot be read
   */
  private static @Modifiable Map<String, Path> relativeFileNames(Path dir) throws IOException {
    Map<String, Path> result = new TreeMap<>();
    try (Stream<Path> paths = Files.walk(dir)) {
      for (Path path : paths.toList()) {
        if (Files.isRegularFile(path)) {
          result.put(dir.relativize(path).toString().replace(File.separatorChar, '/'), path);
        }
      }
    }
    return result;
  }

  /**
   * Returns the contents of the given file, as UTF-8 text.
   *
   * @param file the file to read
   * @return the contents of the given file
   * @throws IOException if the file cannot be read
   */
  private static String readText(Path file) throws IOException {
    return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
  }

  /**
   * Returns the contents of the given goal file, or the empty string if the goal file does not
   * exist.
   *
   * @param file the goal file to read
   * @return the contents of the given goal file, or the empty string
   * @throws IOException if the file cannot be read
   */
  private static String goalText(Path file) throws IOException {
    return Files.isRegularFile(file) ? normalizeLineSeparators(readText(file)) : "";
  }

  /**
   * Returns the given standard output or standard error of the program, normalized for comparison
   * with a goal file: line separators are normalized, and each occurrence of the name of the
   * working directory is replaced by {@link #workingDirPlaceholder}.
   *
   * @param text what the program wrote to standard output or standard error
   * @param workingDir the directory in which the program ran
   * @return the given text, normalized
   * @throws IOException if the real path of the working directory cannot be determined
   */
  private static String normalizeOutput(String text, Path workingDir) throws IOException {
    String result = normalizeLineSeparators(text);
    // The program might name the working directory by its real path (in which symbolic links are
    // resolved) or by its absolute path, and with either the platform's separator or "/", as in a
    // URI.  Replace longer names first, so that no name is partially replaced.
    Set<String> names =
        new TreeSet<>(
            Comparator.comparingInt(String::length)
                .reversed()
                .thenComparing(Comparator.naturalOrder()));
    for (Path dir : List.of(workingDir.toRealPath(), workingDir.toAbsolutePath())) {
      String name = dir.toString();
      names.add(name);
      names.add(name.replace(File.separatorChar, '/'));
    }
    for (String name : names) {
      result = result.replace(name, workingDirPlaceholder);
    }
    return result;
  }

  /**
   * Returns the given text with Windows line separators replaced by Unix ones, so that a goal file
   * matches program output on any platform and regardless of which line separator the program
   * writes.
   *
   * @param text the text to normalize
   * @return the given text, with Windows line separators replaced by Unix ones
   */
  private static String normalizeLineSeparators(String text) {
    return text.replace("\r\n", "\n");
  }

  /**
   * Copies a directory tree.
   *
   * @param from the directory to copy
   * @param to the directory to copy it to; it and its parents are created if necessary
   * @throws IOException if a file cannot be copied
   */
  private static void copyTree(Path from, Path to) throws IOException {
    Files.createDirectories(to);
    for (Map.Entry<String, Path> file : relativeFileNames(from).entrySet()) {
      Path target = to.resolve(file.getKey().replace('/', File.separatorChar));
      Path parent = target.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.copy(file.getValue(), target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
