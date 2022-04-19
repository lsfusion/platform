package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import static lsfusion.gwt.client.base.GwtClientUtils.javaScriptExceptionHandler;

public interface CustomCellEditor extends RequestValueCellEditor { // ,RequestValueCellEditor but it's class not an interface

    default Element getCustomElement(Element parent) {
        return parent;
    }

    String getRenderFunction();
    JavaScriptObject getCustomEditor();

    default void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize, Object oldValue) {
        javaScriptExceptionHandler(() -> CustomReplaceCellEditor.render(getRenderFunction(), getCustomEditor(), getCustomElement(cellParent),
                CustomReplaceCellEditor.getController(this, cellParent), ARequestValueCellEditor.fromObject(oldValue)));
    }

    default void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        javaScriptExceptionHandler(() -> CustomReplaceCellEditor.clear(getCustomEditor(), getCustomElement(cellParent), cancel));
    }

    default Object getValue(Element parent, Integer contextAction) {
        return javaScriptExceptionHandler(() -> CustomReplaceCellEditor.getValue(getCustomEditor(), getCustomElement(parent))); // "canceled" if we want to cancel
    }

    default void onBrowserEvent(Element parent, EventHandler handler) {
        javaScriptExceptionHandler(() -> CustomReplaceCellEditor.onBrowserEvent(getCustomEditor(), handler.event, getCustomElement(parent)));
    }
}
