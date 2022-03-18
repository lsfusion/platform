package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.CustomCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.CustomReplaceCellEditor;

import java.util.function.Consumer;

public class CustomCellRenderer extends CellRenderer<Object> {
    private final JavaScriptObject customRenderer;

    public CustomCellRenderer(GPropertyDraw property, String customRenderFunction) {
        super(property);
        this.customRenderer = CustomReplaceCellEditor.getCustomFunction(customRenderFunction);
    }

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
        render(customRenderer, element);
    }

    protected native void render(JavaScriptObject customRenderer, Element element)/*-{
        customRenderer.render(element);
    }-*/;

    @Override
    public void renderDynamicContent(Element element, Object value, UpdateContext updateContext) {
        setRendererValue(customRenderer, element,
                getController(updateContext.getCustomRendererValueChangeConsumer(), updateContext.isPropertyReadOnly()),
                GSimpleStateTableView.convertValue(property, value));
    }

    protected native void setRendererValue(JavaScriptObject customRenderer, Element element, JavaScriptObject controller, JavaScriptObject value)/*-{
        customRenderer.update(element, controller, value);
    }-*/;

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        clear(customRenderer, element);
    }
    
    protected native void clear(JavaScriptObject customRenderer, Element element)/*-{
        if (customRenderer.clear !== undefined)
            customRenderer.clear(element);
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
            },
            getColorThemeName: function () {
                return @lsfusion.gwt.client.view.MainFrame::colorTheme.@java.lang.Enum::name()();
            }
        }
    }-*/;

    protected native final <T> JavaScriptObject fromObject(T object) /*-{
        return object;
    }-*/;

    protected native final <T> T toObject(JavaScriptObject object) /*-{
        return object;
    }-*/;

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
