package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class WriteResourcesJSPTag extends TagSupport {

    private List<MainController.WebAction> resources;

    public void setResources(List<MainController.WebAction> resources) {
        this.resources = resources;
    }

    @Override
    public int doStartTag() throws JspException {
        if (resources != null) {
            try {
                JspWriter out = pageContext.getOut();
                // should be pretty similar to GwtActionDispatcher.executeFile
                for (MainController.WebAction webAction : resources) {
                    String s = webAction.resource;
                    String extension = webAction.extension;
                    if (extension.equals("js")) {
                        out.print("<script type='text/javascript' src='" + s + "'></script>");
                    } else if (extension.equals("css")) {
                        out.print("<link rel='stylesheet' type='text/css' href='" + s + "' />");
                    } else if (extension.equals("html")) { //to add tags to header.
                        out.print(s);
                    } else if (SystemUtils.isFont(extension)) {
                        out.print("<script>"
                                + "var fontFace = new FontFace('" + webAction.resourceName + "', 'url(" + s + ")');"
                                + "fontFace.load().then(function(loadedFace) {"
                                + "    document.fonts.add(loadedFace);"
                                + "    }"
                                + ");"
                                + "</script>");
                    } else {
                        out.print("<script>window.lsfFiles = window.lsfFiles || {}; window.lsfFiles['" + webAction.resourceName + "'] = '" + s + "';</script>");
                    }
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        return super.doStartTag();
    }
}
