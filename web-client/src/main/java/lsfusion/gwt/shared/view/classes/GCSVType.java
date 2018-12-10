package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;

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
