package lsfusion.client.form.editor.rich;

import lsfusion.base.ReflectionUtils;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Enumeration;

public class RichEditorWriter extends HTMLWriter {
    private static final Method convertToHTML = ReflectionUtils.getPrivateMethod(HTMLWriter.class, "convertToHTML", AttributeSet.class, MutableAttributeSet.class);

    public RichEditorWriter(Writer w, HTMLDocument doc, int pos, int len) {
        super(w, doc, pos, len);
    }

    protected void writeAttributes(AttributeSet attr) throws IOException {
        MutableAttributeSet mutable = ReflectionUtils.invokeMethod(convertToHTML, this, attr, null);

        String align = (String) mutable.getAttribute(HTML.Attribute.ALIGN);
        if (align != null) {
            mutable.removeAttribute(HTML.Attribute.ALIGN);
            String style = (String) mutable.getAttribute(HTML.Attribute.STYLE);
            if (style == null) {
                style = "";
            } else {
                style = style + ";";
            }
            style += "text-align: " + align;
            mutable.addAttribute(HTML.Attribute.STYLE, style);
        }

        Enumeration names = mutable.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            if (name instanceof HTML.Tag ||
                name instanceof StyleConstants ||
                name == HTML.Attribute.ENDTAG) {
                continue;
            }
            write(" " + name + "=\"" + mutable.getAttribute(name) + "\"");
        }
    }
}
