package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;

public class GCustomStaticFormatFileType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeCustomStaticFormatFileCaption() + ": " + GwtSharedUtils.toString(",", extensions.toArray());
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }
}
