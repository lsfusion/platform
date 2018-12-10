package lsfusion.gwt.form.shared.view.classes.link;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.classes.GTypeVisitor;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;

public class GPDFLinkType extends GLinkType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typePDFFileLinkCaption();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}