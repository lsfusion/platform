package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public abstract class AbstractGridCellEditor implements GridCellEditor {
    @Override
    public void renderDom(Element cellParent, RenderContext renderContext, UpdateContext updateContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replaceCellRenderer() {
        return false;
    }
}
