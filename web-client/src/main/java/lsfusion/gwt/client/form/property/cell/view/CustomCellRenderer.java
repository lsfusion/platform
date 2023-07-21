package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.CustomReplaceCellEditor;
import lsfusion.interop.action.ServerResponse;

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

    protected static void getAsyncValues(String value, UpdateContext updateContext, JavaScriptObject successCallBack, JavaScriptObject failureCallBack) {
        updateContext.getAsyncValues(value, ServerResponse.CHANGE, GSimpleStateTableView.getJSCallback(successCallBack, failureCallBack));
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
            changeProperty: function (propertyName, object, newValue, remove) { // important that implementation should guarantee that the position of the object should be relevant (match the list)
                var controller = this;
                return this.change({
                    property : propertyName,
                    objects : this.getObjects(object),
                    value : newValue
                }, function(oldValue) {
                    if(oldValue == null)
                        oldValue = [];
                    var objectsString = controller.getObjectsString(object);
                    var testFunction = function (oldObject) { return controller.getObjectsString(oldObject) === objectsString; };
                    var newArray = remove ? $wnd.removeObjectFromArray(oldValue, testFunction) : $wnd.replaceOrAddObjectFieldInArray(oldValue, testFunction, propertyName, newValue, object);
                    @GSimpleStateTableView::setDiffPrev(*)(newArray, element);
                    return newArray;
                });
            },
            getValues: function(value, successCallback, failureCallback) {
                return @CustomCellRenderer::getAsyncValues(*)(value, updateContext, successCallback, failureCallback);
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
            diff: function (newList, element, fnc, noDiffObjects, removeFirst) {
                var controller = this;
                @GSimpleStateTableView::diff(*)(newList, element, fnc, function(object) {return controller.getObjectsString(object);}, this.getObjectsField(), noDiffObjects, removeFirst);
            },
            isList: function () {
                return false;
            },
            getPropertyValues: function(property, value, successCallback, failureCallback) {
                return this.getValues(property + ":" + value, successCallback, failureCallback); // should be compatible with JSONProperty.AsyncMapJSONChange.getAsyncValueList
            },
            getObjects: function (object) {
                return object.objects;
            },
            getObjectsField: function () {
                return "objects";
            },
            getObjectsString: function (object) {
                return @GwtClientUtils::jsonStringify(*)(this.getObjects(object));
            },
            createObject: function (object, objects) {
                return $wnd.replaceField(object, "objects", objects);
            }
        }
    }-*/;

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
