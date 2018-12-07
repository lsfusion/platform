package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;

public class GCSVType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeCSVFileCaption();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}
