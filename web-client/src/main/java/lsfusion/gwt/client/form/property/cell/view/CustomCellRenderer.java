package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.object.table.grid.view.GStateTableView;
import lsfusion.gwt.client.form.object.table.grid.view.DiffObjectInterface;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.CustomReplaceCellEditor;

public class CustomCellRenderer extends CellRenderer {
    private final JavaScriptObject customRenderer;

    public CustomCellRenderer(GPropertyDraw property, String customRenderFunction) {
        super(property);
        this.customRenderer = CustomReplaceCellEditor.getCustomFunction(customRenderFunction);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        render(customRenderer, element);

        return false;
    }

    protected native void render(JavaScriptObject customRenderer, Element element)/*-{
        customRenderer.render(element);
    }-*/;

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        JavaScriptObject renderValue = GSimpleStateTableView.convertToJSValue(property, value);
        setRendererValue(customRenderer, element, getController(property, updateContext, element), renderValue);

        return false;
    }

    protected native void setRendererValue(JavaScriptObject customRenderer, Element element, JavaScriptObject controller, JavaScriptObject value)/*-{
        customRenderer.update(element, controller, value);
    }-*/;

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        clear(customRenderer, element);

        return false;
    }
    
    protected native void clear(JavaScriptObject customRenderer, Element element)/*-{
        if (customRenderer.clear !== undefined)
            customRenderer.clear(element);
    }-*/;

    @Override
    public String format(PValue value) {
        return PValue.getStringValue(value);
    }

    protected static void changeValue(Element element, UpdateContext updateContext, JavaScriptObject value, GPropertyDraw property, JavaScriptObject renderValueSupplier) {
        GType externalChangeType = property.getExternalChangeType();

        boolean canUseChangeValueForRendering = renderValueSupplier != null || property.canUseChangeValueForRendering(externalChangeType);
        if(!canUseChangeValueForRendering) // to break a recursion when there are several changes in update
            rerenderState(element, false);

        updateContext.changeProperty(GSimpleStateTableView.convertFromJSUndefValue(externalChangeType, value),
                renderValueSupplier != null ? (oldValue, changeValue) -> GSimpleStateTableView.convertFromJSValue(property.baseType, GwtClientUtils.call(renderValueSupplier, GSimpleStateTableView.convertToJSValue(property.baseType, oldValue))) : null);

        // if we don't use change value for rendering, and the renderer is interactive (it's state can be changed without notifying the element)
        // there might be a problem that this change might be grouped with the another change that will change the state to the previous value, but update won't be called (because of caching), which is usually an "unexpected behaviour"
        // disabling caching at all will lead to dropping state after, for example, refresh

        // it's important to do it after changeProperty
        // 1. to break a recursion execute -> setLoading -> update -> change -> execute (if there is a change in update)
        // 2. to avoid dropping custom element state
        if(!canUseChangeValueForRendering)
            rerenderState(element, true);
    }

    private static GwtClientUtils.JavaScriptObjectWrapper getKey(JavaScriptObject object) {
        return new GwtClientUtils.JavaScriptObjectWrapper(object);
    }

    public static JavaScriptObject getController(GPropertyDraw property, UpdateContext updateContext, Element element) {
        return getController(property, updateContext, element, updateContext.isPropertyReadOnly());
    }

    private static native JavaScriptObject getController(GPropertyDraw property, UpdateContext updateContext, Element element, Boolean isReadOnly)/*-{
        return {
            change: function (value, renderValueSupplier) {
                if(value === undefined) // not passed
                    value = @GSimpleStateTableView::UNDEFINED;
                return @CustomCellRenderer::changeValue(*)(element, updateContext, value, property, renderValueSupplier);
            },
            changeValue: function (value) { // deprecated
                return this.change(value);
            },
            changeProperty: function (propertyName, object, newValue) {
                return this.change({
                    property : propertyName,
                    object : object,
                    value : newValue
                }, function(oldValue) {
                    return $wnd.replaceObjectFieldInArray(oldValue, object, propertyName, newValue);
                });
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
            },
            diff: function (newList, element, fnc, updateKey) {
                @GSimpleStateTableView::diff(*)(newList, element, fnc, updateKey);
            },
            isList: function () {
                return false;
            },
            getKey: function (object) {
                return @CustomCellRenderer::getKey(*)(object);
            }
        }
    }-*/;

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
