package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.controller.GFormController;
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
        public final String defaultValue;

        public ExtraValue(String placeholder, Boolean readonly, String defaultValue) {
            this.placeholder = placeholder;
            this.readonly = readonly;
            this.defaultValue = defaultValue;
        }

        public JavaScriptObject getJsObject() {
            return getJsObject(placeholder, readonly, defaultValue);
        }
        protected native JavaScriptObject getJsObject(String placeholder, Boolean readonly, String defaultValue)/*-{
            return {
                placeholder: placeholder,
                readonly: readonly,
                defaultValue: defaultValue
            };
        }-*/;

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof ExtraValue && GwtClientUtils.nullEquals(readonly, ((ExtraValue) o).readonly)
                    && GwtClientUtils.nullEquals(placeholder, ((ExtraValue) o).placeholder)
                    && GwtClientUtils.nullEquals(defaultValue, ((ExtraValue) o).defaultValue);
        }

        @Override
        public int hashCode() {
            return GwtClientUtils.nullHash(defaultValue) * 31 * 31 + GwtClientUtils.nullHash(placeholder) * 31 + (readonly != null ? (readonly ? 2 : 1) : 0);
        }
    }

    @Override
    protected Object getExtraValue(UpdateContext updateContext) {
        boolean customNeedPlaceholder = property.customNeedPlaceholder;
        boolean customNeedReadonly = property.customNeedReadonly;
        boolean customNeedDefaultValue = property.customNeedDefaultValue;
        if(customNeedPlaceholder || customNeedReadonly || customNeedDefaultValue)
            return new ExtraValue(customNeedPlaceholder ? updateContext.getPlaceholder() : null,
                    customNeedReadonly ? updateContext.isPropertyReadOnly() : null, customNeedDefaultValue ? updateContext.getDefaultValue() : null);

        return super.getExtraValue(updateContext);
    }

    protected native void render(JavaScriptObject customRenderer, JavaScriptObject controller, Element element)/*-{
        customRenderer.render(element, controller);
    }-*/;

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        // closed even when the application's own renderer throws - see GCustom.onUpdate for why a transaction left
        // open is not a local failure
        FocusUtils.startFocusTransaction(element);
        try {
            JavaScriptObject renderValue = GSimpleStateTableView.convertToJSValue(property, value, updateContext.getRendererType(), true);
            update(customRenderer, element, getController(property, updateContext, element), renderValue, extraValue != null ? ((ExtraValue) extraValue).getJsObject() : null);
        } finally {
            FocusUtils.endFocusTransaction();
        }

        return false;
    }

    protected native void update(JavaScriptObject customRenderer, Element element, JavaScriptObject controller, JavaScriptObject value, JavaScriptObject extraValue)/*-{
        customRenderer.update(element, controller, value, extraValue);
    }-*/;

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        clear(customRenderer, element, getRenderController(property, renderContext, element));

        CustomCellRenderer.clearCustomElement(element);

        return false;
    }
    
    protected native void clear(JavaScriptObject customRenderer, Element element, JavaScriptObject controller)/*-{
        if (customRenderer.clear !== undefined)
            customRenderer.clear(element, controller);
    }-*/;

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        return PValue.getStringValue(value);
    }

    protected static void getAsyncValues(String value, UpdateContext updateContext, JavaScriptObject successCallBack, JavaScriptObject failureCallBack, int increaseValuesNeededCount) {
        updateContext.getAsyncValues(value, ServerResponse.CHANGE, GFormController.getJSCallback(successCallBack, failureCallBack), increaseValuesNeededCount);
    }

    protected static void changeValue(Element element, UpdateContext updateContext, JavaScriptObject value, GPropertyDraw property, JavaScriptObject renderValueSupplier) {
        RendererType rendererType = updateContext.getRendererType();

        boolean canUseChangeValueForRendering = renderValueSupplier != null || property.hasExternalChangeActionForRendering(rendererType);
        if(!canUseChangeValueForRendering) // to break a recursion when there are several changes in update
            rerenderState(element, false);

        updateContext.changeProperty(GSimpleStateTableView.convertFromJSUndefValue(property, value),
                renderValueSupplier != null ? (oldValue, changeValue) -> {
                    GType renderType = property.getRenderType(rendererType);
                    return GSimpleStateTableView.convertFromJSValue(renderType, GwtClientUtils.call(renderValueSupplier, GSimpleStateTableView.convertToJSValue(renderType, property, true, oldValue)));
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
        return getController(property, updateContext, element, updateContext.isPropertyReadOnly(), updateContext.isTabFocusable(), updateContext.getForm().controller);
    }

    protected static boolean previewEvent(Element element, Event event, UpdateContext updateContext) {
        return updateContext.previewEvent(element, event);
    }

    private static native JavaScriptObject getController(GPropertyDraw property, UpdateContext updateContext, Element element, Boolean isReadOnly, boolean isTabFocusable, JavaScriptObject formController)/*-{
        return {
            change: function (value, renderValueSupplier) {
                if(value === undefined) // not passed
                    value = @GwtClientUtils::UNDEFINED;
                return @CustomCellRenderer::changeValue(*)(element, updateContext, value, property, renderValueSupplier);
            },
            changeValue: function (value) { // deprecated
                return this.change(value);
            },
            changeProperty: function (propertyName, object, newValue, type, index, changeValue) { // important that implementation should guarantee that the position of the object should be relevant (match the list)
                var controller = this;
                return this.change((changeValue === undefined ? {
                    property : propertyName,
                    objects : this.getObjects(object),
                    value : newValue
                } : changeValue), function(oldValue) {
                    return @GSimpleStateTableView::changeJSDiff(*)(element, oldValue != null ? oldValue : [], object, controller, propertyName, newValue, type, index);
                });
            },
            getValues: function(value, successCallback, failureCallback, increaseValuesNeededCount) {
                return @CustomCellRenderer::getAsyncValues(*)(value, updateContext, successCallback, failureCallback, increaseValuesNeededCount != null ? increaseValuesNeededCount : 0);
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
            toTimeDTO: function (hour, minute, second, millisecond) {
                return @lsfusion.gwt.client.form.property.cell.classes.GTimeDTO::new(IIII)(hour, minute, second, millisecond);
            },
            toDateTimeDTO: function (year, month, day, hour, minute, second, millisecond) {
                return @lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO::new(IIIIIII)(year, month, day, hour, minute, second, millisecond);
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
            getPropertyValues: function(property, value, successCallback, failureCallback, increaseValuesNeededCount) {
                return this.getValues(property + ":" + value, successCallback, failureCallback, increaseValuesNeededCount); // should be compatible with JSONProperty.AsyncMapJSONChange.getAsyncValueList
            },
            getObjects: function (object) {
                return object.objects;
            },
            getObjectsField: function () {
                return @lsfusion.gwt.client.form.object.GGroupObjectValue::ROW_OBJECTS;
            },
            // a PLATFORM row is identified by one rule, not by a JSON dump of its handle, which made one row two
            // different rows depending on who asked. The rule is the canonical key string, named by the objects it is
            // OF: this list is the author's own and is scoped to no group, so two rows of DIFFERENT groups holding the
            // same value would otherwise diff as one row. A clone keeps the handle (it is enumerable), so it is that
            // row and says so. One that has LOST the handle is not that row to the platform either - it resolves to
            // nothing - so it falls to `key`, which is the best it can still say about itself, not a claim to be the
            // row it was copied from. Only then the author's own list items,
            // which keep the dump - they have no platform identity to be canonical about. The middle step is the loose
            // one: an author item carrying a field called `key` is taken at its word, so two of them sharing that
            // value diff as one item.
            getObjectsString: function (object) {
                var k = @lsfusion.gwt.client.form.object.GGroupObjectValue::resolveObject(*)(object);
                if (k !== null) return k.@lsfusion.gwt.client.form.object.GGroupObjectValue::toIdentityString()();
                if (object !== null && typeof object === 'object' && object.key !== undefined) return String(object.key);
                return @GwtClientUtils::jsonStringify(*)(this.getObjects(object));
            },
            // a platform handle mints a proper row - key and handle stamped together, instead of the handle replaced
            // and the cloned key left pointing at another row. An author's own handle is kept as it is: it is theirs
            createObject: function (object, objects) {
                var k = @lsfusion.gwt.client.form.object.GGroupObjectValue::resolveObject(*)(objects);
                if (k !== null) return @lsfusion.gwt.client.form.object.GGroupObjectValue::createRow(*)(object, objects);
                return $wnd.replaceField(object, @lsfusion.gwt.client.form.object.GGroupObjectValue::ROW_OBJECTS, objects);
            },
            isRenderInputKeyEvent: function (event, multiLine) {
                return @lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer::isInputKeyEvent(*)(event, updateContext, multiLine);
            },
            isEditInputKeyEvent: function (event, multiLine) {
                return @lsfusion.gwt.client.form.property.cell.classes.controller.InputBasedCellEditor::isInputKeyEvent(*)(event, multiLine);
            },
            previewEvent: function (element, event) {
                return @CustomCellRenderer::previewEvent(*)(element, event, updateContext);
            },
            form: formController
        }
    }-*/;

    public static JavaScriptObject getRenderController(GPropertyDraw property, RenderContext renderContext, Element element) {
        return getRenderController(renderContext, element, renderContext.isTabFocusable());
    }

    private static native JavaScriptObject getRenderController(RenderContext renderContext, Element element, boolean isTabFocusable)/*-{
        var thisObj = this;
        return {
            isList: function () {
                return false;
            },
            isTabFocusable: function () {
                return isTabFocusable;
            },
            clearDiff: function () {
                @GSimpleStateTableView::clearDiff(*)(element);
            }
        };
    }-*/;

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
