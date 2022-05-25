package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.GPropertyDraw;
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
    public boolean renderDynamicContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        setRendererValue(customRenderer, element,
                getController(updateContext::changeProperty, element, updateContext.isPropertyReadOnly()),
                GSimpleStateTableView.convertToJSValue(property, value));

        return false;
    }

    @Override
    protected boolean needToRenderToolbarContent() {
        return false;
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
    
    protected void changeValue(Element element, Consumer<Object> valueChangeConsumer, JavaScriptObject value) {
        GType externalChangeType = property.getExternalChangeType();

        // if we don't use change value for rendering, and the renderer is interactive (it's state can be changed without notifying the element)
        // there might be a problem that this change might be grouped with the another change that will change the state to the previous value, but update won't be called (because of caching), which is usually an "unexpected behaviour"
        // disabling caching at all will lead to dropping state after, for example, refresh
        if(!property.canUseChangeValueForRendering(externalChangeType))
            rerenderState(element);

        valueChangeConsumer.accept(GSimpleStateTableView.convertFromJSValue(externalChangeType, value));
    }

    protected native JavaScriptObject getController(Consumer<Object> valueChangeConsumer, Element element, Boolean isReadOnly)/*-{
        var thisObj = this;
        return {
            change: function (value) {
                return thisObj.@CustomCellRenderer::changeValue(*)(valueChangeConsumer, element, value);
            },
            changeValue: function (value) { // deprecated
                return thisObj.@CustomCellRenderer::changeValue(*)(valueChangeConsumer, element, value);
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

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
