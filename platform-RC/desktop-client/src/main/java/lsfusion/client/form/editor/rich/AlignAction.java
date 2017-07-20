package lsfusion.client.form.editor.rich;

import lsfusion.base.ReflectionUtils;
import net.atlanticbb.tantlinger.i18n.I18n;
import net.atlanticbb.tantlinger.ui.UIUtils;
import net.atlanticbb.tantlinger.ui.text.CompoundUndoManager;
import net.atlanticbb.tantlinger.ui.text.HTMLUtils;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLTextEditAction;
import org.bushe.swing.action.ActionManager;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.event.ActionEvent;


/**
 * c/p from lsfusion.client.form.editor.rich.AlignAction to fix bug with caret
 */
public class AlignAction extends HTMLTextEditAction {
    private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.ui.text.actions");

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;
    public static final int JUSTIFY = 3;

    public static final String ALIGNMENT_NAMES[] = {
            i18n.str("left"),
            i18n.str("center"),
            i18n.str("right"),
            i18n.str("justify")
    };

    private static final int[] MNEMS = {
            i18n.mnem("left"),
            i18n.mnem("center"),
            i18n.mnem("right"),
            i18n.mnem("justify")
    };

    public static final String ALIGNMENTS[] = {
            "left", "center", "right", "justify"
    };

    private static final String IMGS[] = {
            "al_left.png", "al_center.png", "al_right.png", "al_just.png"
    };

    private int align;


    /**
     * Creates a new AlignAction
     * @param al LEFT, RIGHT, CENTER, or JUSTIFY
     * @throws IllegalArgumentException
     */
    public AlignAction(int al) throws IllegalArgumentException {
        super("");
        if (al < 0 || al >= ALIGNMENTS.length) {
            throw new IllegalArgumentException("Illegal Argument");
        }

        //String pkg = getClass().getPackage().getName();
        putValue(NAME, (ALIGNMENT_NAMES[al]));
        putValue(MNEMONIC_KEY, MNEMS[al]);

        putValue(SMALL_ICON, UIUtils.getIcon(UIUtils.X16, IMGS[al]));
        putValue(ActionManager.BUTTON_TYPE, ActionManager.BUTTON_TYPE_VALUE_RADIO);

        align = al;
    }

    protected void updateWysiwygContextState(JEditorPane ed) {
        setSelected(shouldBeSelected(ed));
    }

    private boolean shouldBeSelected(JEditorPane ed) {
        HTMLDocument document = (HTMLDocument) ed.getDocument();
        Element elem = document.getParagraphElement(ed.getCaretPosition());
        if (HTMLUtils.isImplied(elem)) {
            elem = elem.getParentElement();
        }

        AttributeSet at = elem.getAttributes();
        return at.containsAttribute(HTML.Attribute.ALIGN, ALIGNMENTS[align]);
    }

    protected void updateSourceContextState(JEditorPane ed) {
        setSelected(false);
    }

    protected void wysiwygEditPerformed(ActionEvent e, JEditorPane editor) {
        HTMLDocument doc = (HTMLDocument) editor.getDocument();
        Element curE = doc.getParagraphElement(editor.getSelectionStart());
        Element endE = doc.getParagraphElement(editor.getSelectionEnd());
        //System.err.println("ALIGN " + curE.getName());

        int caret = editor.getCaretPosition();
        CompoundUndoManager.beginCompoundEdit(doc);
        while (true) {
            alignElement(curE);
            if (curE.getEndOffset() >= endE.getEndOffset()
                || curE.getEndOffset() >= doc.getLength()) {
                break;
            }
            curE = doc.getParagraphElement(curE.getEndOffset() + 1);
        }
        editor.setCaretPosition(caret);
        CompoundUndoManager.endCompoundEdit(doc);
    }

    private void alignElement(Element elem) {
        HTMLDocument doc = (HTMLDocument) elem.getDocument();

        if (HTMLUtils.isImplied(elem)) {
            HTML.Tag tag = HTML.getTag(elem.getParentElement().getName());
            //System.out.println(tag);
            //pre tag doesn't support an align attribute
            //http://www.w3.org/TR/REC-html32#pre
            if (tag != null && (!tag.equals(HTML.Tag.BODY)) &&
                (!tag.isPreformatted() && !tag.equals(HTML.Tag.DD))) {
                SimpleAttributeSet as = new SimpleAttributeSet(elem.getAttributes());
                as.removeAttribute("align");
                as.addAttribute("align", ALIGNMENTS[align]);

                Element parent = elem.getParentElement();
                String html = HTMLUtils.getElementHTML(elem, false);
                html = HTMLUtils.createTag(tag, as, html);
                String snipet = "";
                for (int i = 0; i < parent.getElementCount(); i++) {
                    Element el = parent.getElement(i);
                    if (el == elem) {
                        snipet += html;
                    } else {
                        snipet += HTMLUtils.getElementHTML(el, true);
                    }
                }

                try {
                    doc.setOuterHTML(parent, snipet);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            //Set the HTML attribute on the paragraph...
            SimpleAttributeSet set = new SimpleAttributeSet(elem.getAttributes());
            
            set.removeAttribute(HTML.Attribute.ALIGN);
            set.removeAttribute(StyleConstants.Alignment);
            set.removeAttribute(CSS.Attribute.TEXT_ALIGN);
            set.addAttribute(StyleConstants.Alignment, align);

            //Set the paragraph attributes...
            int start = elem.getStartOffset();
            int length = elem.getEndOffset() - elem.getStartOffset();
            doc.setParagraphAttributes(start, length - 1, set, true);
        }
    }
    
    private static final CSS.Attribute textAlignAttr =
            ReflectionUtils.createByPrivateConstructor(
                    CSS.Attribute.class,
                    new Class[] {String.class, String.class, Boolean.TYPE},
                    new Object[] {"text-align", null, true}
            );

    protected void sourceEditPerformed(ActionEvent e, JEditorPane editor) {
        String prefix = "<p align=\"" + ALIGNMENTS[align] + "\">";
        String postfix = "</p>";
        String sel = editor.getSelectedText();
        if (sel == null) {
            editor.replaceSelection(prefix + postfix);

            int pos = editor.getCaretPosition() - postfix.length();
            if (pos >= 0) {
                editor.setCaretPosition(pos);
            }
        } else {
            sel = prefix + sel + postfix;
            editor.replaceSelection(sel);
        }
    }
}
