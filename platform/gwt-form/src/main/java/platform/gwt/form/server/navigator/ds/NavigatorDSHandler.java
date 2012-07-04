package platform.gwt.form.server.navigator.ds;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;
import platform.client.logics.DeSerializer;
import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorForm;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.base.server.spring.NavigatorProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class NavigatorDSHandler implements HttpRequestHandler {

    @Autowired
    private NavigatorProvider navigatorProvider;

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            DeSerializer.deserializeListClientNavigatorElementWithChildren(navigatorProvider.getNavigator().getNavigatorTree());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            blProvider.invalidate();
        }

        ClientNavigatorElement node = ClientNavigatorElement.root;

        StringBuffer xml = new StringBuffer();
        xml.append("<nodes>\n");
        serializeToXml(xml, "none", node);
        xml.append("</nodes>\n");

        response.setContentType("text/xml; charset=UTF-8");
        response.getWriter().print(xml.toString());
    }

    private void serializeToXml(StringBuffer xml, String parentSID, ClientNavigatorElement node) {
        boolean isForm = node instanceof ClientNavigatorForm;
        xml.append("\t<node>\n")
                .append("\t\t<elementSid>").append(node.sID).append("</elementSid>\n")
                .append("\t\t<parentSid>").append(parentSID).append("</parentSid>\n")
                .append("\t\t<isForm>").append(isForm).append("</isForm>\n")
                .append("\t\t<icon>").append(isForm ? "form.png" : "open.png").append("</icon>\n")
                .append("\t\t<caption>").append(node.caption).append("</caption>\n")
                .append("\t</node>\n");

        for (ClientNavigatorElement child : node.children) {
            serializeToXml(xml, node.sID, child);
        }
    }
}
