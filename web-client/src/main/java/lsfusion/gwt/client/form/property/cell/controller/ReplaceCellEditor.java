package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public interface ReplaceCellEditor extends CellEditor {

    default boolean needReplace(Element cellParent, RenderContext renderContext) {
        return true;
    }

    void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize, Object oldValue);

    void clearRender(Element cellParent, RenderContext renderContext, boolean cancel);
}
