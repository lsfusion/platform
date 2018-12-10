package lsfusion.gwt.shared.form.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;

public class GExcelType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeExcelFileCaption();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}
