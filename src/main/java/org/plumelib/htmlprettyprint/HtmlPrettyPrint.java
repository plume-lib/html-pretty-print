package org.plumelib.htmlprettyprint;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Pretty-prints an HTML file, after converting it to valid XML. To use:
 *
 * <pre>java org.plumelib.htmlprettyprint.HtmlPrettyPrint file.html &gt; filepp.html</pre>
 */
public final class HtmlPrettyPrint {

  /** This class is a collection of methods; it does not represent anything. */
  private HtmlPrettyPrint() {
    throw new Error("do not instantiate");
  }

  /**
   * Entry point for the HtmlPrettyPrint program. Pretty-prints each HTML file named on the command
   * line, writing the result to standard output. The exit status is 1 if any file could not be
   * processed, and 0 otherwise.
   *
   * @param args the HTML files to pretty-print
   */
  public static void main(String[] args) {

    Serializer serializer = new Serializer(System.out);
    serializer.setIndent(2);
    serializer.setMaxLength(80);

    Builder parser = makeParser();

    int status = 0;
    for (String arg : args) {
      String url = new File(arg).toURI().toString();

      try {
        prettyPrint(parser, serializer, url);
      } catch (ParsingException ex) {
        System.err.println(url + " is not well-formed.");
        status = 1;
      } catch (IOException ex) {
        System.err.println("Could not read or write " + url + ": " + ex);
        status = 1;
      }
    }

    System.exit(status);
  }

  /**
   * Returns a parser that uses TagSoup to read possibly-malformed HTML.
   *
   * @return an HTML parser
   */
  static Builder makeParser() {
    try {
      XMLReader tagsoup =
          SAXParserFactory.newInstance("org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl", null)
              .newSAXParser()
              .getXMLReader();
      return new Builder(tagsoup);
    } catch (ParserConfigurationException | SAXException ex) {
      throw new Error("Could not create HTML parser", ex);
    }
  }

  /**
   * Pretty-prints the HTML read from {@code url}, writing the result to {@code serializer}.
   *
   * @param parser the HTML parser, as produced by {@link #makeParser}
   * @param serializer where to write the pretty-printed HTML
   * @param url the location of the HTML to read
   * @throws ParsingException if the input is not well-formed
   * @throws IOException if there is trouble reading the input or writing the output
   */
  static void prettyPrint(Builder parser, Serializer serializer, String url)
      throws ParsingException, IOException {
    Document document = parser.build(url);
    serializer.write(document);
    serializer.flush();
  }
}
