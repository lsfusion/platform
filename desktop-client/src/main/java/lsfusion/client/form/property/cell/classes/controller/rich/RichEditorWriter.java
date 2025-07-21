package lsfusion.client.form.property.cell.classes.controller.rich;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

public class RichEditorWriter extends HTMLWriter {

    public RichEditorWriter(Writer w, HTMLDocument doc, int pos, int len) {
        super(w, doc, pos, len);
        setLineLength(Integer.MAX_VALUE);
    }

    protected void writeAttributes(AttributeSet attr) throws IOException {
        MutableAttributeSet mutable = convertToHTMLCustom(attr);

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

    //c/p private methods of HTMLWriter
    private final MutableAttributeSet convAttrCustom = new SimpleAttributeSet();

    private MutableAttributeSet convertToHTMLCustom(AttributeSet from) {
        convAttrCustom.removeAttributes(convAttrCustom);
        convertToHTML32Custom(from, convAttrCustom);
        return convAttrCustom;
    }

    private static void convertToHTML32Custom(AttributeSet from, MutableAttributeSet to) {
        if (from == null) {
            return;
        }
        Enumeration<?> keys = from.getAttributeNames();
        StringBuilder value = new StringBuilder();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof CSS.Attribute) {
                if ((key == CSS.Attribute.FONT_FAMILY) ||
                        (key == CSS.Attribute.FONT_SIZE) ||
                        (key == CSS.Attribute.COLOR)) {

                    createFontAttributeCustom((CSS.Attribute)key, from, to);
                } else if (key == CSS.Attribute.FONT_WEIGHT) {
                    // add a bold tag is weight is bold
                    Object weightValue = from.getAttribute(CSS.Attribute.FONT_WEIGHT);
                    if ((weightValue != null) && (String.valueOf(weightValue).equals("bold"))) {
                        addAttributeCustom(to, HTML.Tag.B, SimpleAttributeSet.EMPTY);
                    }
                } else if (key == CSS.Attribute.FONT_STYLE) {
                    String s = from.getAttribute(key).toString();
                    if (s.contains("italic")) {
                        addAttributeCustom(to, HTML.Tag.I, SimpleAttributeSet.EMPTY);
                    }
                } else if (key == CSS.Attribute.TEXT_DECORATION) {
                    String decor = from.getAttribute(key).toString();
                    if (decor.contains("underline")) {
                        addAttributeCustom(to, HTML.Tag.U, SimpleAttributeSet.EMPTY);
                    }
                    if (decor.contains("line-through")) {
                        addAttributeCustom(to, HTML.Tag.STRIKE, SimpleAttributeSet.EMPTY);
                    }
                } else if (key == CSS.Attribute.VERTICAL_ALIGN) {
                    String vAlign = from.getAttribute(key).toString();
                    if (vAlign.contains("sup")) {
                        addAttributeCustom(to, HTML.Tag.SUP, SimpleAttributeSet.EMPTY);
                    }
                    if (vAlign.contains("sub")) {
                        addAttributeCustom(to, HTML.Tag.SUB, SimpleAttributeSet.EMPTY);
                    }
                } else if (key == CSS.Attribute.TEXT_ALIGN) {
                    addAttributeCustom(to, HTML.Attribute.ALIGN,
                            from.getAttribute(key).toString());
                } else {
                    // default is to store in an HTML style attribute
                    if (value.length() > 0) {
                        value.append("; ");
                    }
                    value.append(key).append(": ").append(from.getAttribute(key));
                }
            } else {
                Object attr = from.getAttribute(key);
                if (attr instanceof AttributeSet) {
                    attr = ((AttributeSet)attr).copyAttributes();
                }
                addAttributeCustom(to, key, attr);
            }
        }
        if (value.length() > 0) {
            to.addAttribute(HTML.Attribute.STYLE, value.toString());
        }
    }

    private static void createFontAttributeCustom(CSS.Attribute a, AttributeSet from,
                                            MutableAttributeSet to) {
        MutableAttributeSet fontAttr = (MutableAttributeSet)
                to.getAttribute(HTML.Tag.FONT);
        if (fontAttr == null) {
            fontAttr = new SimpleAttributeSet();
            to.addAttribute(HTML.Tag.FONT, fontAttr);
        }
        // edit the parameters to the font tag
        String htmlValue = from.getAttribute(a).toString();
        if (a == CSS.Attribute.FONT_FAMILY) {
            fontAttr.addAttribute(HTML.Attribute.FACE, htmlValue);
        } else if (a == CSS.Attribute.FONT_SIZE) {
            fontAttr.addAttribute(HTML.Attribute.SIZE, htmlValue);
        } else if (a == CSS.Attribute.COLOR) {
            fontAttr.addAttribute(HTML.Attribute.COLOR, htmlValue);
        }
    }

    private static void addAttributeCustom(MutableAttributeSet to, Object key, Object value) {
        Object attr = to.getAttribute(key);
        if (attr == null || attr == SimpleAttributeSet.EMPTY) {
            to.addAttribute(key, value);
        } else {
            if (attr instanceof MutableAttributeSet &&
                    value instanceof AttributeSet) {
                ((MutableAttributeSet)attr).addAttributes((AttributeSet)value);
            }
        }
    }
}
