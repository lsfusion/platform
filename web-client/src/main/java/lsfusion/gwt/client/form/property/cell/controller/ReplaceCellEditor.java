package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public interface ReplaceCellEditor extends CellEditor {

    void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize);

    void clearRender(Element cellParent, RenderContext renderContext);
}
