package lsfusion.server.logics.form.stat.struct.hierarchy.xml;

import lsfusion.base.file.RawFileData;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.xml.ImportXMLAction;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// IMPORT XML reads a file: a document may declare a DTD, but nothing outside the document is read
public class XMLNodeParseTest {

    private static final String EXTRA_DTD = "<!ATTLIST name extra CDATA \"FROM-DTD\">";

    private static RawFileData file(String xml) {
        return new RawFileData(xml.getBytes(StandardCharsets.UTF_8));
    }

    // a file is read with no charset (the one of the XML header) or with a charset given, both the same way
    private static String value(String xml) throws Exception {
        String value = ImportXMLAction.findRootNode(file(xml), null, null).getChild("name").getValue();
        assertEquals(value, ImportXMLAction.findRootNode(file(xml), null, "UTF-8").getChild("name").getValue());
        return value;
    }

    private static String extra(String xml) throws Exception {
        String extra = ImportXMLAction.findRootNode(file(xml), null, null).getChild("name").getAttributeValue("extra");
        assertEquals(extra, ImportXMLAction.findRootNode(file(xml), null, "UTF-8").getChild("name").getAttributeValue("extra"));
        return extra;
    }

    // the parser stops at one of its limits: the message starts with the limit's code, whatever the locale
    private static void assertStopped(String xml, String charset, String[] codes) throws Exception {
        String message = null;
        try {
            ImportXMLAction.findRootNode(file(xml), null, charset);
        } catch (Exception e) {
            assertEquals("JDOMParseException", e.getClass().getSimpleName());
            message = e.getMessage();
        }
        assertNotNull("a document over the parser's limits is read", message);
        assertTrue(message, Arrays.stream(codes).anyMatch(message::contains));
    }

    private static void assertStopped(String xml, String... codes) throws Exception {
        assertStopped(xml, null, codes);
        assertStopped(xml, "UTF-8", codes);
    }

    private static String fileURI(String content) throws Exception {
        File file = File.createTempFile("xmlnode", ".tmp");
        file.deleteOnExit();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file.toURI().toString();
    }

    // a file that is not there: reading it would fail the parse
    private static String missingURI() throws Exception {
        File file = File.createTempFile("xmlnode", ".tmp");
        Files.delete(file.toPath());
        return file.toURI().toString();
    }

    private static String generalEntity(String uri) {
        return "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY s SYSTEM \"" + uri + "\">]><r><name>&s;</name></r>";
    }

    private static String externalDTD(String uri) {
        return "<?xml version=\"1.0\"?><!DOCTYPE r SYSTEM \"" + uri + "\"><r><name>v</name></r>";
    }

    private static String parameterEntity(String uri) {
        return "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY % p SYSTEM \"" + uri + "\"> %p;]><r><name>v</name></r>";
    }

    private static String repeat(String s, int times) {
        return new String(new char[times]).replace("\0", s);
    }

    @Test
    public void externalEntityIsNotRead() throws Exception {
        assertFalse(value(generalEntity(fileURI("FILE-CONTENT"))).contains("FILE-CONTENT"));
        value(generalEntity(missingURI()));
    }

    @Test
    public void externalDTDIsNotRead() throws Exception {
        assertNull(extra(externalDTD(fileURI(EXTRA_DTD))));
        extra(externalDTD(missingURI()));
    }

    @Test
    public void externalParameterEntityIsNotRead() throws Exception {
        assertNull(extra(parameterEntity(fileURI(EXTRA_DTD))));
        extra(parameterEntity(missingURI()));
    }

    @Test
    public void entityExpansionsAreLimitedInNumber() throws Exception {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY e0 \"x\">");
        for (int i = 1; i <= 7; i++) // 10^7 expansions
            xml.append("<!ENTITY e").append(i).append(" \"").append(repeat("&e" + (i - 1) + ";", 10)).append("\">");
        xml.append("]><r><name>&e7;</name></r>");
        assertStopped(xml.toString(), "JAXP00010001"); // entity expansions
    }

    @Test
    public void entityExpansionsAreLimitedInSize() throws Exception {
        // 60 expansions of a 10^6 characters entity
        assertStopped("<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY big \"" + repeat("x", 1000000) + "\">]><r><name>" + repeat("&big;", 60) + "</name></r>",
                "JAXP00010003", "JAXP00010004"); // the size of an entity, the size of all entities
    }

    @Test
    public void charsetIsKept() throws Exception {
        String text = "\u043f\u0440\u0438\u0432\u0435\u0442";
        assertEquals(text, ImportXMLAction.findRootNode(new RawFileData(("<?xml version=\"1.0\" encoding=\"windows-1251\"?><r><name>" + text + "</name></r>").getBytes("windows-1251")), null, null).getChild("name").getValue());
        assertEquals(text, ImportXMLAction.findRootNode(new RawFileData(("<r><name>" + text + "</name></r>").getBytes("windows-1251")), null, "windows-1251").getChild("name").getValue());
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] xml = ("<r><name>" + text + "</name></r>").getBytes(StandardCharsets.UTF_8);
        byte[] withBOM = Arrays.copyOf(bom, bom.length + xml.length);
        System.arraycopy(xml, 0, withBOM, bom.length, xml.length);
        assertEquals(text, ImportXMLAction.findRootNode(new RawFileData(withBOM), null, null).getChild("name").getValue());
        assertEquals(text, ImportXMLAction.findRootNode(new RawFileData(withBOM), null, "UTF-8").getChild("name").getValue());
    }

    @Test
    public void internalDTDIsKept() throws Exception {
        assertEquals("INNER", extra("<?xml version=\"1.0\"?><!DOCTYPE r [<!ATTLIST name extra CDATA \"INNER\">]><r><name>v</name></r>"));
        assertEquals("a-inner-b", value("<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY i \"-inner-\">]><r><name>a&i;b</name></r>"));
        assertEquals("<x&", value("<?xml version=\"1.0\"?><!DOCTYPE r [<!ELEMENT r ANY>]><r><name>&lt;x&amp;</name></r>"));
    }
}
