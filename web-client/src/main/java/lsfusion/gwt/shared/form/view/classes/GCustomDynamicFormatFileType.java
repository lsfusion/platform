package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;

public class GCustomDynamicFormatFileType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeCustomDynamicFormatFileCaption();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}
