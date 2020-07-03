package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public interface ReplaceCellEditor extends CellEditor {

    void renderDom(Element cellParent, RenderContext renderContext, UpdateContext updateContext);
}
