package lsfusion.client.form.editor.rich;

import org.bushe.swing.action.ActionList;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static net.atlanticbb.tantlinger.ui.text.HTMLUtils.*;

public class RichEditorUtils {
    /**
     * Set's the font family at the {@link JEditorPane}'s current caret positon, or
     * for the current selection (if there is one).
     * <p>
     * If the fontName parameter is null, any currently set font family is removed.
     * </p>
     * @param editor
     * @param fontName
     */
    public static void setFontFamily(JEditorPane editor, String fontName) {
        AttributeSet attr = getCharacterAttributes(editor);
        if (attr == null) {
            return;
        }

        if (fontName == null) {
            //we're removing the font

            //the font might be defined as a font tag
            Object val = attr.getAttribute(HTML.Tag.FONT);
            if (val != null && val instanceof AttributeSet) {
                MutableAttributeSet set = new SimpleAttributeSet((AttributeSet) val);
                val = set.getAttribute(HTML.Attribute.FACE); //does it have a FACE attrib?
                if (val != null) {
                    set.removeAttribute(HTML.Attribute.FACE);
                    removeCharacterAttribute(editor, HTML.Tag.FONT); //remove the current font tag
                    if (set.getAttributeCount() > 0) {
                        //it's not empty so replace the other font attribs
                        SimpleAttributeSet fontSet = new SimpleAttributeSet();
                        fontSet.addAttribute(HTML.Tag.FONT, set);
                        setCharacterAttributes(editor, set);
                    }
                }
            }
            //also remove these for good measure
            removeCharacterAttribute(editor, StyleConstants.FontFamily);
            removeCharacterAttribute(editor, CSS.Attribute.FONT_FAMILY);
        } else {
            //adding the font family
            MutableAttributeSet tagAttrs = new SimpleAttributeSet();
            tagAttrs.addAttribute(StyleConstants.FontFamily, fontName);
            setCharacterAttributes(editor, tagAttrs);
        }
    }

    public static void setFontSize(JEditorPane editor, Integer fontSize) {
        if (fontSize == null) {
            removeCharacterAttribute(editor, StyleConstants.FontSize);
        } else {
            new StyledEditorKit.FontSizeAction("", fontSize).actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    static int getFontSize(JEditorPane editor) {
        AttributeSet attr = getCharacterAttributes(editor);
        if (attr != null) {
            Object val = attr.getAttribute(StyleConstants.FontSize);
            if (val != null) {
                return (Integer) val;
            }
        }
        return -1;
    }

    static Action[] toActionArray(ActionList actionList) {
        List actions = new ArrayList();
        for (Object v : actionList) {
            if (v != null && v instanceof Action) {
                actions.add(v);
            }
        }

        return (Action[]) actions.toArray(new Action[actions.size()]);
    }
}
