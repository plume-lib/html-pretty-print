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
 * <pre>java plume.HtmlPrettyPrint file.html &gt; filepp.html</pre>
 */
public final class HtmlPrettyPrint {

  /** This class is a collection of methods; it does not represent anything. */
  private HtmlPrettyPrint() {
    throw new Error("do not instantiate");
  }

  /**
   * Entry point for the HtmlPrettyPrint program.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    int status = 0;

    for (String arg : args) {
      File f = new File(arg);
      String url = "file://" + f.getAbsolutePath();

      try {
        XMLReader tagsoup =
            SAXParserFactory.newInstance("org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl", null)
                .newSAXParser()
                .getXMLReader();
        Builder parser = new Builder(tagsoup);

        // Parse the document
        Document document = parser.build(url);

        Serializer serializer = new Serializer(System.out);
        serializer.setIndent(2);
        serializer.setMaxLength(80);
        try {
          serializer.write(document);
        } catch (IOException ex) {
          System.err.println(ex);
          status = 1;
        }
      } catch (ParserConfigurationException | SAXException ex) {
        System.out.println(ex);
        status = 1;
      } catch (ParsingException ex) {
        System.out.println(url + " is not well-formed.");
        throw new Error(ex);
      } catch (IOException ex) {
        System.out.println("IOException:  parser could not read " + url);
        status = 1;
      }
    }
  }
}
