package lsfusion.gwt.form.shared.view.classes.link;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GTypeVisitor;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;
import lsfusion.gwt.form.client.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.client.grid.renderer.ImageLinkGridCellRenderer;

public class GImageLinkType extends GLinkType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageLinkGridCellRenderer(property);
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeImageLinkCaption();
    }
}