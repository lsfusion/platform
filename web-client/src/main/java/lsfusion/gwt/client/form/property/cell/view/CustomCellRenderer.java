package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.base.GwtClientUtils;
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
        render(element);
    }

    protected native void render(Element element)/*-{
        $wnd[this.@CustomCellRenderer::customRenderFunction]().render(element);
    }-*/;

    @Override
    public void renderDynamicContent(Element element, Object value, UpdateContext updateContext) {
        setRendererValue(element,
                getController(updateContext.getCustomRendererValueChangeConsumer(), updateContext.isPropertyReadOnly()),
                fromObject(value));
    }

    protected native void setRendererValue(Element element, JavaScriptObject controller, JavaScriptObject value)/*-{
        $wnd[this.@CustomCellRenderer::customRenderFunction]().update(element, controller, value);
    }-*/;

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        clear(element);
    }
    
    protected native void clear(Element element)/*-{
        $wnd[this.@CustomCellRenderer::customRenderFunction]().clear(element);
    }-*/;

    @Override
    public String format(Object value) {
        return value == null ? "" : value.toString();
    }
    
    protected void changeValue(Consumer<Object> valueChangeConsumer, JavaScriptObject value) {
        if (valueChangeConsumer != null) {
            valueChangeConsumer.accept(toObject(value));
        }
    }

    protected native JavaScriptObject getController(Consumer<Object> valueChangeConsumer, Boolean isReadOnly)/*-{
        var thisObj = this;
        return {
            changeValue: function (value) {
                return thisObj.@CustomCellRenderer::changeValue(*)(valueChangeConsumer, value);
            },
            isReadOnly: function () {
                return isReadOnly;
            },
            toDateDTO: function (year, month, day) {
                return @lsfusion.gwt.client.form.property.cell.classes.GDateDTO::new(III)(year, month, day);
            },
            toTimeDTO: function (hour, minute, second) {
                return @lsfusion.gwt.client.form.property.cell.classes.GTimeDTO::new(III)(hour, minute, second);
            },
            toDateTimeDTO: function (year, month, day, hour, minute, second) {
                return @lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO::new(IIIIII)(year, month, day, hour, minute, second);
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
