package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public interface ReplaceCellEditor extends CellEditor {

    default boolean needReplace(Element cellParent, RenderContext renderContext) {
        return true;
    }

    void render(Element cellParent, RenderContext renderContext, PValue oldValue, Integer renderedWidth, Integer renderedHeight);

    void clearRender(Element cellParent, RenderContext renderContext, boolean cancel);
}
