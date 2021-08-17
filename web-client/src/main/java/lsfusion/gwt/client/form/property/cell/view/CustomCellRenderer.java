package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.function.Consumer;

public class CustomCellRenderer extends CellRenderer<Object> {
    private final String renderFunction;
    private final String setValueFunction;
    private final String clearFunction;

    public CustomCellRenderer(GPropertyDraw property, String customRenderFunction) {
        super(property);

        if (!customRenderFunction.contains(":")) {
            String[] functionString = customRenderFunction.split("\\.");
            customRenderFunction = getPredefinedRenderFunctionsString(functionString[0], functionString.length == 2 ? functionString[1] : null);
        }

        int firstColonIndex = customRenderFunction.indexOf(":");
        int secondColonIndex = customRenderFunction.lastIndexOf(":");
        renderFunction = customRenderFunction.substring(0, firstColonIndex);
        setValueFunction = customRenderFunction.substring(firstColonIndex + 1, secondColonIndex);
        clearFunction = customRenderFunction.substring(secondColonIndex + 1);
    }

    protected native String getPredefinedRenderFunctionsString(String functionName, String functionParam)/*-{
        return $wnd[functionName](functionParam);
    }-*/;

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
        render(element);
    }

    protected native void render(Element element)/*-{
        $wnd[this.@CustomCellRenderer::renderFunction](element);
    }-*/;

    @Override
    public void renderDynamicContent(Element element, Object value, UpdateContext updateContext) {
        setRendererValue(element,
                getController(fromObject(updateContext.getCustomRendererValueChangeConsumer()), updateContext.isPropertyReadOnly()),
                fromObject(value));
    }

    protected native void setRendererValue(Element element, JavaScriptObject controller, JavaScriptObject value)/*-{
        $wnd[this.@CustomCellRenderer::setValueFunction](element, value, controller);
    }-*/;

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        clear(element);
    }
    
    protected native void clear(Element element)/*-{
        $wnd[this.@CustomCellRenderer::clearFunction](element);
    }-*/;

    @Override
    public String format(Object value) {
        return value == null ? "" : value.toString();
    }
    
    protected void changeValue(JavaScriptObject event, JavaScriptObject valueChangeConsumerObject, JavaScriptObject value) {
        if (event != null)
            GwtClientUtils.stopPropagation(this.<NativeEvent>toObject(event));
        
        Consumer<Object> valueChangeConsumer = this.toObject(valueChangeConsumerObject);
        if (valueChangeConsumer != null) {
            valueChangeConsumer.accept(toObject(value));
        }
    }

    protected native JavaScriptObject getController(JavaScriptObject valueChangeConsumer, Boolean isReadOnly)/*-{
        var thisObj = this;
        return {
            changeValue: function (event, value) {
                return thisObj.@CustomCellRenderer::changeValue(*)(event, valueChangeConsumer, value);
            },
            isReadOnly: function () {
                return isReadOnly;
            },
            toDateDTO: function (year, month, day) {
                return @lsfusion.gwt.client.form.property.cell.classes.GDateDTO::new(III)(year, month, day);
            },
            toTimeDTO: function (hour, minute, second) {
                return @lsfusion.gwt.client.form.property.cell.classes.GTimeDTO::new(III)(hour, minute, second);
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
