package org.plumelib.htmlprettyprint;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import nu.xom.Builder;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link HtmlPrettyPrint}. */
final class HtmlPrettyPrintTest {

  /**
   * Pretty-prints the given HTML and returns the result.
   *
   * @param tempDir a temporary directory in which to place the input file
   * @param html the HTML to pretty-print
   * @return the pretty-printed HTML
   * @throws ParsingException if the input is not well-formed
   * @throws IOException if there is trouble reading or writing
   */
  private static String prettyPrint(Path tempDir, String html)
      throws ParsingException, IOException {
    Path htmlFile = tempDir.resolve("input.html");
    Files.writeString(htmlFile, html);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Serializer serializer = new Serializer(baos);
    serializer.setIndent(2);
    serializer.setMaxLength(80);

    Builder parser = HtmlPrettyPrint.makeParser();
    HtmlPrettyPrint.prettyPrint(parser, serializer, htmlFile.toUri().toString());

    return baos.toString(StandardCharsets.UTF_8);
  }

  @Test
  void prettyPrintsWellFormedHtml(@TempDir Path tempDir) throws ParsingException, IOException {
    String output = prettyPrint(tempDir, "<html><body><p>Hello</p></body></html>");
    assertTrue(output.contains("Hello"), output);
    assertTrue(output.contains("<body"), output);
    assertTrue(output.contains("</html>"), output);
    assertTrue(output.contains("\n"), output);
  }

  @Test
  void repairsMalformedHtml(@TempDir Path tempDir) throws ParsingException, IOException {
    // TagSoup closes the unclosed tags and supplies the missing html/body structure.
    String output = prettyPrint(tempDir, "<p>unclosed<p>tags");
    assertTrue(output.contains("unclosed"), output);
    assertTrue(output.contains("tags"), output);
    assertTrue(output.contains("</html>"), output);
  }
}
