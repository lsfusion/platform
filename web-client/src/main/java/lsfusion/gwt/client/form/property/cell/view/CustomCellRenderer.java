package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
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
        CustomCellRenderer.setCustomElement(element);

        render(customRenderer, getRenderController(property, renderContext, element), element);

        return true;
    }

    @Override
    public boolean canBeRenderedInTD() {
        return property.customCanBeRenderedInTD;
    }

    public static class ExtraValue {
        public final String placeholder;
        public final Boolean readonly;

        public ExtraValue(String placeholder, Boolean readonly) {
            this.placeholder = placeholder;
            this.readonly = readonly;
        }

        public JavaScriptObject getJsObject() {
            return getJsObject(placeholder, readonly);
        }
        protected native JavaScriptObject getJsObject(String placeholder, Boolean readonly)/*-{
            return {
                placeholder: placeholder,
                readonly: readonly
            };
        }-*/;

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof ExtraValue && GwtClientUtils.nullEquals(readonly, ((ExtraValue) o).readonly) && GwtClientUtils.nullEquals(placeholder, ((ExtraValue) o).placeholder);
        }

        @Override
        public int hashCode() {
            return GwtClientUtils.nullHash(placeholder) * 31 + (readonly != null ? (readonly ? 2 : 1) : 0);
        }
    }

    @Override
    protected Object getExtraValue(UpdateContext updateContext) {
        boolean customNeedPlaceholder = property.customNeedPlaceholder;
        boolean customNeedReadonly = property.customNeedReadonly;
        if(customNeedPlaceholder || customNeedReadonly)
            return new ExtraValue(customNeedPlaceholder ? updateContext.getPlaceholder() : null, customNeedReadonly ? updateContext.isPropertyReadOnly() : null);

        return super.getExtraValue(updateContext);
    }

    protected native void render(JavaScriptObject customRenderer, JavaScriptObject controller, Element element)/*-{
        customRenderer.render(element, controller);
    }-*/;

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        JavaScriptObject renderValue = GSimpleStateTableView.convertToJSValue(property, value, updateContext.getRendererType());
        setRendererValue(customRenderer, element, getController(property, updateContext, element), renderValue, extraValue != null ? ((ExtraValue) extraValue).getJsObject() : null);

        return false;
    }

    protected native void setRendererValue(JavaScriptObject customRenderer, Element element, JavaScriptObject controller, JavaScriptObject value, JavaScriptObject extraValue)/*-{
        customRenderer.update(element, controller, value, extraValue);
    }-*/;

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        clear(customRenderer, element);

        CustomCellRenderer.clearCustomElement(element);

        return false;
    }
    
    protected native void clear(JavaScriptObject customRenderer, Element element)/*-{
        if (customRenderer.clear !== undefined)
            customRenderer.clear(element);
    }-*/;

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        return PValue.getStringValue(value);
    }

    protected static void getAsyncValues(String value, UpdateContext updateContext, JavaScriptObject successCallBack, JavaScriptObject failureCallBack) {
        updateContext.getAsyncValues(value, ServerResponse.CHANGE, GSimpleStateTableView.getJSCallback(successCallBack, failureCallBack));
    }

    protected static void changeValue(Element element, UpdateContext updateContext, JavaScriptObject value, GPropertyDraw property, JavaScriptObject renderValueSupplier) {
        GType externalChangeType = property.getExternalChangeType();
        RendererType rendererType = updateContext.getRendererType();

        boolean canUseChangeValueForRendering = renderValueSupplier != null || property.canUseChangeValueForRendering(externalChangeType, rendererType);
        if(!canUseChangeValueForRendering) // to break a recursion when there are several changes in update
            rerenderState(element, false);

        updateContext.changeProperty(GSimpleStateTableView.convertFromJSUndefValue(externalChangeType, value),
                renderValueSupplier != null ? (oldValue, changeValue) -> {
                    GType renderType = property.getRenderType(rendererType);
                    return GSimpleStateTableView.convertFromJSValue(renderType, GwtClientUtils.call(renderValueSupplier, GSimpleStateTableView.convertToJSValue(renderType, oldValue)));
                } : null);

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
        return getController(property, updateContext, element, updateContext.isPropertyReadOnly(), updateContext.isTabFocusable());
    }

    protected static boolean previewEvent(Element element, Event event, UpdateContext updateContext) {
        return updateContext.previewEvent(element, event);
    }

    private static native JavaScriptObject getController(GPropertyDraw property, UpdateContext updateContext, Element element, Boolean isReadOnly, boolean isTabFocusable)/*-{
        return {
            change: function (value, renderValueSupplier) {
                if(value === undefined) // not passed
                    value = @GSimpleStateTableView::UNDEFINED;
                return @CustomCellRenderer::changeValue(*)(element, updateContext, value, property, renderValueSupplier);
            },
            changeValue: function (value) { // deprecated
                return this.change(value);
            },
            changeProperty: function (propertyName, object, newValue, type, index) { // important that implementation should guarantee that the position of the object should be relevant (match the list)
                var controller = this;
                return this.change({
                    property : propertyName,
                    objects : this.getObjects(object),
                    value : newValue
                }, function(oldValue) {
                    return @GSimpleStateTableView::changeJSDiff(*)(element, oldValue != null ? oldValue : [], object, controller, propertyName, newValue, type, index);
                });
            },
            getValues: function(value, successCallback, failureCallback) {
                return @CustomCellRenderer::getAsyncValues(*)(value, updateContext, successCallback, failureCallback);
            },
            isReadOnly: function () { // extraValue.readonly + customNeedReadonly should be used instead if readonly depends on the data (otherwise it won't be updated)
                return isReadOnly;
            },
            isTabFocusable: function () {
                return isTabFocusable;
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
            diff: function (newList, fnc, noDiffObjects, removeFirst) {
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
            },
            isRenderInputKeyEvent: function (event, multiLine) {
                return @lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer::isInputKeyEvent(*)(event, updateContext, multiLine);
            },
            isEditInputKeyEvent: function (event, multiLine) {
                return @lsfusion.gwt.client.form.property.cell.classes.controller.InputBasedCellEditor::isInputKeyEvent(*)(event, multiLine);
            },
            previewEvent: function (element, event) {
                return @CustomCellRenderer::previewEvent(*)(element, event, updateContext);
            }
        }
    }-*/;

    public static JavaScriptObject getRenderController(GPropertyDraw property, RenderContext renderContext, Element element) {
        return getRenderController(renderContext.getForm().getDropdownParent());
    }

    private static native JavaScriptObject getRenderController(Element dropdownParent)/*-{
        var thisObj = this;
        return {
            isList: function () {
                return false;
            },
            getDropdownParent: function() {
                return dropdownParent;
            }
        };
    }-*/;

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
