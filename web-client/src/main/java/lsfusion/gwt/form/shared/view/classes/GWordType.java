package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;

public class GWordType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeWordFileCaption();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}
