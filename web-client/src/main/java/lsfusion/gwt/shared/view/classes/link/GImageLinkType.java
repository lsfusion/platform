package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.classes.GTypeVisitor;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;
import lsfusion.gwt.client.form.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.form.grid.renderer.ImageLinkGridCellRenderer;

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