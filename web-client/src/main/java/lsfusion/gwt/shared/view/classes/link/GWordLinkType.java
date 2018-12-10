package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.view.classes.GTypeVisitor;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;

public class GWordLinkType extends GLinkType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeWordFileLinkCaption();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}