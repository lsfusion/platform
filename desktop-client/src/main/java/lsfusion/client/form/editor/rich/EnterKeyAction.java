package lsfusion.client.form.editor.rich;

import net.atlanticbb.tantlinger.ui.text.CompoundUndoManager;
import net.atlanticbb.tantlinger.ui.text.HTMLUtils;
import net.atlanticbb.tantlinger.ui.text.actions.DecoratedTextAction;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;

/**
 * changed net.atlanticbb.tantlinger.ui.text.actions.EnterKeyAction to work correctly with RichEditorWriter
 */
public class EnterKeyAction extends DecoratedTextAction {
    /**
     * Creates a new EnterKeyAction.
     * @param defaultEnterAction Should be the default action
     */
    public EnterKeyAction(Action defaultEnterAction) {
        super("EnterAction", defaultEnterAction);
    }

    public void actionPerformed(ActionEvent e) {
        JEditorPane editor;
        HTMLDocument document;

        try {
            editor = (JEditorPane) getTextComponent(e);
            document = (HTMLDocument) editor.getDocument();
        } catch (ClassCastException ex) {
            // don't know what to do with this type 
            // so pass off the event to the delegate
            getDelegate().actionPerformed(e);
            return;
        }

        Element elem = document.getParagraphElement(editor.getCaretPosition());
        Element parentElem = elem.getParentElement();
        HTML.Tag tag = HTML.getTag(elem.getName());
        HTML.Tag parentTag = HTML.getTag(parentElem.getName());
        int caret = editor.getCaretPosition();

        CompoundUndoManager.beginCompoundEdit(document);
        try {
            if (HTMLUtils.isImplied(elem)) {
                //are we inside a list item?
                if (parentTag.equals(HTML.Tag.LI)) {
                    //does the list item have any contents
                    if (parentElem.getEndOffset() - parentElem.getStartOffset() > 1) {
                        String txt = "";
                        //caret at start of listitem
                        if (caret == parentElem.getStartOffset()) {
                            document.insertBeforeStart(parentElem, toListItem(txt));
                        }//caret in the middle of list item content
                        else if (caret < parentElem.getEndOffset() - 1 && caret > parentElem.getStartOffset()) {
                            int len = parentElem.getEndOffset() - caret;
                            txt = document.getText(caret, len);
                            caret--;// hmmm
                            document.insertAfterEnd(parentElem, toListItem(txt));
                            document.remove(caret, len);
                        } else//caret at end of list item
                        {
                            document.insertAfterEnd(parentElem, toListItem(txt));
                        }

                        editor.setCaretPosition(caret + 1);
                    } else// empty list item
                    {
                        Element listParentElem = HTMLUtils.getListParent(parentElem).getParentElement();
                        //System.out.println(listParentElem.getName());

                        if (isListItem(HTML.getTag(listParentElem.getName())))//nested list
                        {
                            //System.out.println("nested list============");

                            //document.insertAfterEnd(parentElem, (toListItem("")));
                            //editor.setCaretPosition(elem.getEndOffset());
                            HTML.Tag listParentTag = HTML.getTag(HTMLUtils.getListParent(listParentElem).toString());
                            /*HTMLEditorKit.InsertHTMLTextAction a = 
                                new HTMLEditorKit.InsertHTMLTextAction("insert",
                                "", listParentTag, HTML.Tag.LI);                            
                            a.actionPerformed(e);*/
                            int start = parentElem.getStartOffset();

                            Element nextElem = HTMLUtils.getNextElement(document, parentElem);

                            int len = nextElem.getEndOffset() - start;

                            String ml = HTMLUtils.getElementHTML(listParentElem, true);
                            //System.out.println(ml);
                            //System.out.println("------------------");

                            ml = ml.replaceFirst("\\<li\\>\\s*\\<\\/li\\>\\s*\\<\\/ul\\>", "</ul>");
                            ml = ml.replaceFirst("\\<ul\\>\\s*\\<\\/ul\\>", "");
                            //System.out.println(ml);

                            document.setOuterHTML(listParentElem, ml);
                            //document.remove(start, len);
                            //HTMLUtils.removeElement(elem);


                        }//are we directly under a table cell?
                        else if (listParentElem.getName().equals("td")) {
                            //reset the table cell contents nested in a <div>
                            //we do this because otherwise the next table cell would
                            //get deleted!! Perhaps this is a bug in swing's html implemenation?
                            encloseInDIV(listParentElem, document);
                            editor.setCaretPosition(caret + 1);
                        } else //end the list
                        {
                            if (isInList(listParentElem)) {
                                //System.out.println("======nested list============");
                                HTML.Tag listParentTag = HTML.getTag(HTMLUtils.getListParent(listParentElem).toString());
                                HTMLEditorKit.InsertHTMLTextAction a =
                                        new HTMLEditorKit.InsertHTMLTextAction("insert",
                                                                               "<li></li>", listParentTag, HTML.Tag.LI);
                                a.actionPerformed(e);
                            } else {
                                HTML.Tag root = HTML.Tag.BODY;
                                if (HTMLUtils.getParent(elem, HTML.Tag.TD) != null) {
                                    root = HTML.Tag.TD;
                                }

                                HTMLEditorKit.InsertHTMLTextAction a =
                                        new HTMLEditorKit.InsertHTMLTextAction("insert",
                                                                               "<p></p>", root, HTML.Tag.P);
                                a.actionPerformed(e);
                            }

                            HTMLUtils.removeElement(parentElem);
                        }
                    }
                } else //not a list
                {
                    //System.out.println("IMPLIED DEFAULT");
                    //System.out.println("elem: " + elem.getName());
                    //System.out.println("pelem: " + parentElem.getName());

                    if (parentTag.isPreformatted()) {
                        insertImpliedBR(e);
                    } else if (parentTag.equals(HTML.Tag.TD)) {
                        encloseInDIV(parentElem, document);
                        editor.setCaretPosition(caret + 1);
                    } else if (parentTag.equals(HTML.Tag.BODY) || isInList(elem)) {
                        //System.out.println("insertParagraphAfter elem");
                        insertParagraphAfter(elem, editor);
                    } else {
                        //System.out.println("***insertParagraphAfter parentElem");
                        insertParagraphAfter(parentElem, editor);
                    }
                }
            } else //not implied
            {
                //we need to check for this here in case any straggling li's
                //or dd's exist
                if (isListItem(tag)) {
                    if ((elem.getEndOffset() - editor.getCaretPosition()) == 1) {
                        //System.out.println("inserting \\n ");
                        //caret at end of para
                        editor.replaceSelection("\n ");
                        editor.setCaretPosition(editor.getCaretPosition() - 1);
                    } else {
                        //System.out.println("NOT implied delegate");
                        getDelegate().actionPerformed(e);
                    }
                } else {
                    //System.out.println("not implied insertparaafter1 " + elem.getName());
                    insertParagraphAfter(elem, editor);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        CompoundUndoManager.endCompoundEdit(document);
    }

    private boolean isListItem(HTML.Tag t) {
        return (t.equals(HTML.Tag.LI) ||
                t.equals(HTML.Tag.DT) ||
                t.equals(HTML.Tag.DD));
    }

    private String toListItem(String txt) {
        return "<li>" + txt + "</li>";
    }

    private boolean isInList(Element el) {
        return HTMLUtils.getListParent(el) != null;
    }

    private void insertImpliedBR(ActionEvent e) {
        HTMLEditorKit.InsertHTMLTextAction hta =
                new HTMLEditorKit.InsertHTMLTextAction("insertBR",
                                                       "<br>", HTML.Tag.IMPLIED, HTML.Tag.BR);
        hta.actionPerformed(e);
    }

    private void encloseInDIV(Element elem, HTMLDocument document)
            throws Exception {
        //System.out.println("enclosing in div: " + elem.getName());
        HTML.Tag tag = HTML.getTag(elem.getName());
        String html = HTMLUtils.getElementHTML(elem, false);
        html = HTMLUtils.createTag(tag,
                                   elem.getAttributes(), "<div>" + html + "</div><div></div>");

        document.setOuterHTML(elem, html);
    }

    /**
     * Inserts a paragraph after the current paragraph of the same type
     * @param elem
     * @param editor
     * @throws javax.swing.text.BadLocationException
     * @throws IOException
     */
    private void insertParagraphAfter(Element elem, JEditorPane editor) throws BadLocationException, IOException {
        int caretPos = editor.getCaretPosition();
        HTMLDocument document = (HTMLDocument) elem.getDocument();
        HTML.Tag t = HTML.getTag(elem.getName());
        int endOffs = elem.getEndOffset();
        int startOffs = elem.getStartOffset();

        //if this is an implied para, make the new para a div
        if (t == null || elem.getName().equals("p-implied")) {
            t = HTML.Tag.DIV;
        }


        String html;
        //got to test for this here, otherwise <hr> and <br>
        //get duplicated
        if (caretPos == startOffs) {
            html = createBlock(t, elem, "");
        } else {
            //split the current para at the cursor position
            StringWriter out = new StringWriter();
            ElementWriter w = new ElementWriter(out, elem, startOffs, caretPos);
            w.write();
            html = createBlock(t, elem, out.toString());
        }

        if (caretPos == endOffs - 1) {
            html += createBlock(t, elem, "");
        } else {
            StringWriter out = new StringWriter();
            ElementWriter w = new ElementWriter(out, elem, caretPos, endOffs);
            w.write();
            html += createBlock(t, elem, out.toString());
        }

        //copy the current para's character attributes
        AttributeSet chAttribs;
        if (endOffs > startOffs && caretPos == endOffs - 1) {
            chAttribs = new SimpleAttributeSet(document.getCharacterElement(caretPos - 1).getAttributes());
        } else {
            chAttribs = new SimpleAttributeSet(document.getCharacterElement(caretPos).getAttributes());
        }

        document.setOuterHTML(elem, html);

        caretPos++;
        Element p = document.getParagraphElement(caretPos);
        if (caretPos == endOffs) {
            //update the character attributes for the added paragraph
            //FIXME If the added paragraph is at the start/end 
            //of the document, the char attrs dont get set           
            setCharAttribs(p, chAttribs);
        }

        editor.setCaretPosition(p.getStartOffset());
    }

    private void setCharAttribs(Element p, AttributeSet chAttribs) {
        HTMLDocument document = (HTMLDocument) p.getDocument();
        int start = p.getStartOffset();
        int end = p.getEndOffset();

        SimpleAttributeSet sas = new SimpleAttributeSet(chAttribs);
        sas.removeAttribute(HTML.Attribute.SRC);
        //if the charattribs contains a br, hr, or img attribute, it'll erase
        //any content in the paragraph
        boolean skipAttribs = false;
        for (Enumeration ee = sas.getAttributeNames(); ee.hasMoreElements(); ) {
            Object n = ee.nextElement();
            String val = chAttribs.getAttribute(n).toString();
            ////System.out.println(n + " " + val);
            skipAttribs = val.equals("br") || val.equals("hr") || val.equals("img");
        }

        if (!skipAttribs) {
            document.setCharacterAttributes(start, end - start, sas, true);
        }
    }

    private String createBlock(HTML.Tag t, Element prototype, String content) throws IOException {
        content = HTMLUtils.removeEnclosingTags(prototype, content);
        
        StringWriter out = new StringWriter();
        ElementWriter w = new ElementWriter(out, prototype, 0, 100);
        w. writeAttributes(prototype.getAttributes());
        return "<" + t + out.toString() + ">" + content + "</" + t + ">";
    }

    private String tagOpen(HTML.Tag enclTag, AttributeSet set) {
        String t = "<" + enclTag;
        for (Enumeration e = set.getAttributeNames(); e.hasMoreElements(); ) {
            Object name = e.nextElement();
            if (!name.toString().equals("name")) {
                Object val = set.getAttribute(name);
                t += " " + name + "=\"" + val + "\"";
            }
        }

        return t + ">";
    }

}