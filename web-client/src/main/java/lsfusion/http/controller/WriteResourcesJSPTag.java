package lsfusion.http.controller;

import com.google.common.base.Throwables;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Map;

public class WriteResourcesJSPTag extends TagSupport {

    private Map<String, String> resources;

    public void setResources(Map<String, String> resources) {
        this.resources = resources;
    }

    @Override
    public int doStartTag() throws JspException {
        try {
            JspWriter out = pageContext.getOut();
            for (String s : resources.keySet()) {
                String extension = resources.get(s);
                if (extension.equals("js")) {
                    out.print("<script type='text/javascript' src='" + s + "'></script>");
                } else if (extension.equals("css")) {
                    out.print("<link rel='stylesheet' type='text/css' href='" + s + "' />");
                }
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return super.doStartTag();
    }
}
