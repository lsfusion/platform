package lsfusion.client.form.editor.rich;

import com.google.common.base.Throwables;
import net.atlanticbb.tantlinger.ui.text.CompoundUndoManager;
import net.atlanticbb.tantlinger.ui.text.HTMLUtils;
import net.atlanticbb.tantlinger.ui.text.WysiwygHTMLEditorKit;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLTextEditAction;
import net.atlanticbb.tantlinger.ui.text.actions.TabAction;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

public class RichEditorKit extends WysiwygHTMLEditorKit {

    private ViewFactory viewFactory = new RichEditorViewFactory();

    private Set<JEditorPane> installed = new HashSet<>();

    @Override
    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    @Override
    public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException {
        if (doc instanceof HTMLDocument) {
            RichEditorWriter w = new RichEditorWriter(out, (HTMLDocument)doc, pos, len);
            w.write();
        } else {
            super.write(out, doc, pos, len);
        }
    }

    @Override
    public void install(JEditorPane ed) {
        ActionMap actionMap = ed.getActionMap();
        
        Action tabDelegate = actionMap.get("insert-tab");
        actionMap.put("insert-tab", new RichTabAction(TabAction.FORWARD, tabDelegate));
        
        super.install(ed);
        if (!installed.contains(ed)) {
            Action enterAction = actionMap.get("insert-break");
            actionMap.put("insert-break", new EnterKeyAction(enterAction));

            RichPasteAction pasteAction = new RichPasteAction();
            actionMap.put("paste-from-clipboard", pasteAction);
            pasteAction.putContextValue(HTMLTextEditAction.EDITOR, ed);
            
            installed.add(ed);
        }
    }

    public class RichTabAction extends TabAction {
        public RichTabAction(int type, Action defaultTabAction) {
            super(type, defaultTabAction);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = (JEditorPane) getTextComponent(e);
            HTMLDocument document = (HTMLDocument) editor.getDocument();
            Element elem = document.getParagraphElement(editor.getCaretPosition());
            try {
                document.insertAfterStart(elem, "&nbsp;&nbsp;&nbsp;&nbsp;");
            } catch (BadLocationException | IOException e1) {
                getDelegate().actionPerformed(e);
            }
        }
    }

    private static boolean checkFlavor(DataFlavor flavor) {
        return flavor.getHumanPresentableName().equals("text/html") || flavor.getHumanPresentableName().equals("text/plain") || flavor.getHumanPresentableName().equals("Unicode String");
    }
    
    public static class RichPasteAction extends net.atlanticbb.tantlinger.ui.text.actions.PasteAction {
        @Override
        protected void wysiwygEditPerformed(ActionEvent e, JEditorPane editor) {
            HTMLDocument document = (HTMLDocument) editor.getDocument();
            try {
                HTMLEditorKit ekit = (HTMLEditorKit)editor.getEditorKit();
                CompoundUndoManager.beginCompoundEdit(document);
                Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);

                for (DataFlavor flavor : content.getTransferDataFlavors()) {
                    if (String.class.isAssignableFrom(flavor.getRepresentationClass())) {
                        if (checkFlavor(flavor)) {
                            String txt = content.getTransferData(flavor).toString();
                            txt = HTMLUtils.jEditorPaneizeHTML(RichEditorPane.removeInvalidTags(txt));
                            insertHTML(editor, document, ekit, txt, editor.getSelectionStart());
                            return;
                        }
                    }
                }
            } catch (Exception ex) {
                Throwables.propagate(ex);
            } finally {
                CompoundUndoManager.endCompoundEdit(document);
            }
        }
    }
    
    public static Element getBodyParent(Element element) {
        while (element.getParentElement() != null) {
            if ("body".equals(element.getName())) {
                return element;
            } 
            element = element.getParentElement();
        }
        return null;
    }
    
    public static void insertHTML(JEditorPane editor, HTMLDocument doc, HTMLEditorKit kit, String html, int location) {
        try {
            String docString = doc.getText(0, doc.getLength());
            boolean selectedAll = docString.equals(editor.getSelectedText());
            boolean empty = docString.replaceAll("\n", "").isEmpty();
            int selectionStart = editor.getSelectionStart();
            int selectionEnd = editor.getSelectionEnd();
            doc.remove(selectionStart, selectionEnd - selectionStart);
            if (selectedAll || empty) {
                Element bodyParent = getBodyParent(doc.getParagraphElement(editor.getCaretPosition()));
                if (bodyParent != null) {
                    doc.insertAfterStart(bodyParent, html);
                }
            } else {
                StringReader reader = new StringReader(HTMLUtils.jEditorPaneizeHTML(html));
                kit.read(reader, doc, location);   
            }
        } catch (Exception ex) {
            Throwables.propagate(ex);
        }
    }

    @Override
    public void deinstall(JEditorPane ed) {
        if (installed.contains(ed)) {
            Action action = ed.getActionMap().get("insert-break");
            if (action instanceof EnterKeyAction) {
                ed.getActionMap().put("insert-break", ((EnterKeyAction) action).getDelegate());
            }
            installed.remove(ed);
        }
        super.deinstall(ed);
    }
    
    public class RichEditorViewFactory extends WysiwygHTMLFactory {
        @Override
        public View create(Element elem) {
            
            Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (o instanceof HTML.Tag) {
                HTML.Tag kind = (HTML.Tag) o;
                if (kind == HTML.Tag.IMG) {
                    return new CachedImageView(elem);
                }
            }
            return super.create(elem);
        }
    }
}
