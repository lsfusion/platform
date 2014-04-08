package lsfusion.client.form.editor.rich;

import net.atlanticbb.tantlinger.ui.text.WysiwygHTMLEditorKit;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

public class RichEditorKit extends WysiwygHTMLEditorKit {

    private ViewFactory viewFactory = new RichEditorViewFactory();

    private Set<JEditorPane> installed = new HashSet<JEditorPane>();

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
        super.install(ed);
        if (!installed.contains(ed)) {
            Action enterAction = ed.getActionMap().get("insert-break");
            ed.getActionMap().put("insert-break", new EnterKeyAction(enterAction));
            installed.add(ed);
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
