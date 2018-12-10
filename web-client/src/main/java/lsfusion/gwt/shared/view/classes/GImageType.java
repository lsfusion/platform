package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;
import lsfusion.gwt.client.form.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.form.grid.renderer.ImageGridCellRenderer;

public class GImageType extends GFileType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageGridCellRenderer(property);
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeImageCaption();
    }
}
