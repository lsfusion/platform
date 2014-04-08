package lsfusion.client.form.editor.rich;

import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import java.io.Writer;

/**
 * c/p from net.atlanticbb.tantlinger.ui.text.ElementWriter to extend changed RichEditorWriter
 */
public class ElementWriter extends RichEditorWriter {
    private Element root;

    public ElementWriter(Writer out, Element root, int startPos, int endPos) {
        super(out, (HTMLDocument) root.getDocument(),
              getStartPos(root, startPos),
              Math.min(root.getDocument().getLength(),
                       getEndPos(root, endPos) - getStartPos(root, startPos))
        );


        this.root = root;

        //setIndentSpace(0);        
        setLineLength(Integer.MAX_VALUE);
    }

    protected boolean synthesizedElement(Element e) {
        return e.getStartOffset() < getStartOffset() ||
               isAncestor(e, root) || super.synthesizedElement(e);
    }

    private static boolean isAncestor(Element a, Element d) {
        for (Element e = d.getParentElement(); e != null; e = e.getParentElement()) {
            if (e == a) {
                return true;
            }
        }

        return false;
    }

    private static int getStartPos(Element root, int start) {
        if (start >= root.getStartOffset() && start <= root.getEndOffset()) {
            return start;
        }
        return root.getStartOffset();
    }

    private static int getEndPos(Element root, int end) {
        if (end >= root.getStartOffset() && end <= root.getEndOffset()) {
            return end;
        }
        return root.getEndOffset();
    }
}