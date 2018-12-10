package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;

public class GPDFType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typePDFFileCaption();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}
