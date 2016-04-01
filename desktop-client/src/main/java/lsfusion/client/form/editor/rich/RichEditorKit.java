package lsfusion.client.form.editor.rich;

import net.atlanticbb.tantlinger.ui.text.WysiwygHTMLEditorKit;
import net.atlanticbb.tantlinger.ui.text.actions.TabAction;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.event.ActionEvent;
import java.io.IOException;
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
