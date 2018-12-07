package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;

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
