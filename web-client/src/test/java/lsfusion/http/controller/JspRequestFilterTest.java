package lsfusion.http.controller;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

// no JSP is run by a request: web.xml passes every request (not a forward, the views are forwarded to) through the
// filter, and it answers 404 to one the container would hand to its JSP servlet
public class JspRequestFilterTest {

    @Test
    public void webXmlMapsRequestsToTheFilter() throws Exception {
        Document webXml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("src/main/webapp/WEB-INF/web.xml"));

        String filterName = null;
        NodeList filters = webXml.getElementsByTagName("filter");
        for (int i = 0; i < filters.getLength(); i++) {
            Element filter = (Element) filters.item(i);
            if (text(filter, "filter-class").equals(JspRequestFilter.class.getName()))
                filterName = text(filter, "filter-name");
        }
        assertNotNull(filterName);

        List<String> patterns = new ArrayList<>();
        NodeList mappings = webXml.getElementsByTagName("filter-mapping");
        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element) mappings.item(i);
            if (text(mapping, "filter-name").equals(filterName)) {
                assertEquals(0, mapping.getElementsByTagName("dispatcher").getLength()); // REQUEST only, so the views are still forwarded to
                NodeList urlPatterns = mapping.getElementsByTagName("url-pattern");
                for (int j = 0; j < urlPatterns.getLength(); j++)
                    patterns.add(urlPatterns.item(j).getTextContent().trim());
            }
        }
        assertTrue(patterns.contains("/*"));
    }

    @Test
    public void answersNotFoundToAJsp() throws Exception {
        for (String servletPath : new String[]{"/static/x.jsp", "/x.jspx", "/x.jspf", "/x.xsp", "/x.JSP"})
            assertEquals(servletPath, 404, filter(servletPath));
    }

    @Test
    public void passesOtherRequests() throws Exception {
        assertEquals(0, filter("/main")); // a page
        assertEquals(0, filter("/file")); // /file/static/x.jsp: a downloaded file named so is in the path info
        assertEquals(0, filter("/static/css/x.css"));
    }

    // the status the filter answers with, 0 if it passes the request on
    private int filter(String servletPath) throws Exception {
        int[] status = new int[1];
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class},
                (proxy, method, args) -> method.getName().equals("getServletPath") ? servletPath : null);
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class}, (proxy, method, args) -> {
            if (method.getName().equals("sendError"))
                status[0] = (Integer) args[0];
            return null;
        });
        boolean[] passed = new boolean[1];
        new JspRequestFilter().doFilter(request, response, (chainRequest, chainResponse) -> passed[0] = true);
        assertEquals(status[0] == 0, passed[0]);
        return status[0];
    }

    private static String text(Element element, String tag) {
        return element.getElementsByTagName(tag).item(0).getTextContent().trim();
    }
}
