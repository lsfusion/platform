package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public interface CustomCellEditor extends RequestValueCellEditor { // ,RequestValueCellEditor but it's class not an interface

    default Element getCustomElement(Element parent) {
        return parent;
    }

    String getRenderFunction();
    GType getType();
    JavaScriptObject getCustomEditor();

    default void render(Element cellParent, RenderContext renderContext, PValue oldValue, Integer renderedWidth, Integer renderedHeight) {
        CustomReplaceCellEditor.render(getRenderFunction(), getCustomEditor(), getCustomElement(cellParent), CustomReplaceCellEditor.getController(this, cellParent), GSimpleStateTableView.convertToJSValue(getType(), null, oldValue));
    }

    default void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        CustomReplaceCellEditor.clear(getCustomEditor(), getCustomElement(cellParent), cancel);
    }

    default PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException {
        JavaScriptObject value = CustomReplaceCellEditor.getValue(getCustomEditor(), getCustomElement(parent));
        // "canceled" if we want to cancel
        if(GwtClientUtils.isString(value, "canceled"))
            throw new InvalidEditException();

        return GSimpleStateTableView.convertFromJSValue(getType(), value);
    }

    default void onBrowserEvent(Element parent, EventHandler handler) {
        CustomReplaceCellEditor.onBrowserEvent(getCustomEditor(), handler.event, getCustomElement(parent));
    }
}
