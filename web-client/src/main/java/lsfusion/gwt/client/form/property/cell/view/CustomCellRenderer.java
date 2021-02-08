package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.function.Consumer;

public class CustomCellRenderer extends CellRenderer<Object> {
    private final String customRenderFunction;

    public CustomCellRenderer(GPropertyDraw property, String customRenderFunction) {
        super(property);
        this.customRenderFunction = customRenderFunction;
    }

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
        updateRenderer(element,
                getController(null, null),
                null,
                customRenderFunction,
                false);
    }

    @Override
    public void renderDynamicContent(Element element, Object value, UpdateContext updateContext) {
        updateRenderer(element,
                getController(fromObject(updateContext.getCustomRendererPropertyChange()), updateContext.isPropertyReadOnly()),
                fromObject(value),
                customRenderFunction,
                true);
    }

    protected native void updateRenderer(Element element, JavaScriptObject controller, JavaScriptObject value, String renderFunction, boolean dynamicContent)/*-{
        $wnd[renderFunction](element, value, controller, dynamicContent);
    }-*/;

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
    }

    @Override
    public String format(Object value) {
        return value == null ? "" : value.toString();
    }
    
    protected void changeValue(JavaScriptObject valueChangeConsumerObject, JavaScriptObject value) {
        Consumer<Object> valueChangeConsumer = this.toObject(valueChangeConsumerObject);
        if (valueChangeConsumer != null) {
            valueChangeConsumer.accept(toObject(value));
        }
    }

    protected native JavaScriptObject getController(JavaScriptObject valueChangeConsumer, Boolean isReadOnly)/*-{
        var thisObj = this;
        return {
            changeValue: function (value) {
                return thisObj.@CustomCellRenderer::changeValue(*)(valueChangeConsumer, value);
            },
            isReadOnly: function () {
                return isReadOnly;
            }
        }
    }-*/;
    
    
    protected native final <T> JavaScriptObject fromObject(T object) /*-{
        return object;
    }-*/;

    protected native final <T> T toObject(JavaScriptObject object) /*-{
        return object;
    }-*/;
}
