package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageHtmlOrTextType;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.*;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.interop.action.ServerResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class GSimpleStateTableView<P> extends GStateTableView {

    protected final JavaScriptObject controller;

    public GSimpleStateTableView(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(form, grid, tableContainer);

        Element drawElement = getDrawElement();
        this.controller = getController(drawElement, form.controller);
        GwtClientUtils.setZeroZIndex(drawElement);
    }

    @Override
    public void onBrowserEvent(Element target, EventHandler eventHandler) {
//        if((popupObject != null && getPopupElement().isOrHasChild(target))) // if there is a popupElement we'll consider it not to be part of this view (otherwise on mouse change event focusElement.focus works, and popup panel elements looses focus)
//            return;

        Element cellParent = getCellParent(target);
        form.onPropertyBrowserEvent(eventHandler, cellParent, cellParent != null, getTableDataFocusElement(),
                handler -> {}, // no outer context
                handler -> {}, // no edit
                handler -> {}, // no outer context
                handler -> {}, handler -> {}, // no copy / paste for now
                false, true, true
        );
    }

    protected abstract Element getCellParent(Element target);

    private NativeHashMap<String, Column> columnMap;

    // a custom view's controller.changeProperty(prop, ...) for a prop that is NOT a column of this grid (e.g. a
    // PANEL action like edit/delete) is the FORM controller's job (see #1655); the grid path only stamps this grid's
    // row key. isGridProperty gates that delegation to the form controller (which resolves the property form-wide).
    private boolean isGridProperty(String key) {
        return getColumn(key) != null;
    }
    private Column getColumn(String key) {
        Column column = columnMap.get(key);
        if (column == null) {
            for (GPropertyDraw property : form.getPropertyDraws()) {
                // only properties that are columns of this grid: otherwise the parallel-array
                // getters index by properties.indexOf(property) == -1 and throw (the column key is
                // also unknown here, so EMPTY is only meaningful for an in-grid ungrouped property)
                if (key.equals(property.integrationSID) && containsProperty(property)) {
                    column = new Column(property, GGroupObjectValue.EMPTY);
                    break;
                }
            }
        }
        return column;
    }

    @Override
    protected void updateView() {
        columnMap = new NativeHashMap<>();
        JsArray<JavaScriptObject> list = convertToObjectsMixed(getData(columnMap));

        if(popupObject != null && !isCurrentKey(popupKey)) // if another current key set hiding popup
            hidePopup();

        onUpdate(getDrawElement(), list);
    }

    protected abstract void onUpdate(Element element, JsArray<JavaScriptObject> list);

    @Override
    protected Element getRendererAreaElement() {
        return getElement();
    }

    // we need key / value view since pivot
    private JsArray<JsArray<JavaScriptObject>> getData(NativeHashMap<String, Column> columnMap) {
        JsArray<JsArray<JavaScriptObject>> array = JavaScriptObject.createArray().cast();

        array.push(getCaptions(columnMap, null));

        // getting values
        if(keys != null)
            for (GGroupObjectValue key : keys) { // can be null if manual update
                array.push(getValues(key));
            }
        return array;
    }

    private JsArray<JavaScriptObject> getValues(GGroupObjectValue key) {
        JsArray<JavaScriptObject> rowValues = JavaScriptObject.createArray().cast();
        for (int i = 0; i < properties.size(); i++) {
            GPropertyDraw property = properties.get(i);
            List<GGroupObjectValue> propColumnKeys = columnKeys.get(i);
            NativeHashMap<GGroupObjectValue, PValue> propValues = values.get(i);

            for (int j = 0; j < propColumnKeys.size(); j++) {
                GGroupObjectValue columnKey = propColumnKeys.get(j);
                if(checkShowIf(i, columnKey))
                    continue;

                GGroupObjectValue fullKey = key != null ? GGroupObjectValue.getFullKey(key, columnKey) : GGroupObjectValue.EMPTY;

                rowValues.push(convertToJSValue(property, propValues.get(fullKey), RendererType.SIMPLE, !(this instanceof GMap) && !(this instanceof GCalendar))); // we want images in map and calendar
            }
        }
        rowValues.push(fromObject(key));
        return rowValues;
    }
    public static PValue convertFromJSUndefValue(GPropertyDraw property, JavaScriptObject value) {
        return convertFromJSUndefValue(property.getExternalChangeType(), value);
    }
    public static PValue convertFromJSUndefValue(GType type, JavaScriptObject value) {
        if(GwtClientUtils.isUndefined(value))
            return PValue.UNDEFINED;

        return convertFromJSValue(type, value);
    }
    public static PValue convertFromJSValue(GType type, JavaScriptObject value) {
        // have to reverse convertToJSValue as well as convertFileValue (???)
        // NB: null/undefined MUST be tested on the raw JS value via JSNI (isUndefinedOrNull), never with a Java
        // `value == null`: a JS primitive 0 / false / "" carried in a JavaScriptObject reference reads as null under
        // GWT's `== null`, so a pushed 0 (or empty string, or 3-state false) would be silently dropped to NULL
        if (type instanceof GLogicalType) {
            if(!((GLogicalType) type).threeState)
                return PValue.getPValue(toBoolean(value));

            return PValue.getPValue(!GwtClientUtils.isUndefinedOrNull(value) ? toBoolean(value) : null);
        }
        if(GwtClientUtils.isUndefinedOrNull(value))
            return null;
        if(type instanceof GIntegralType)
            return ((GIntegralType) type).fromDoubleValue(toDouble(value));
        if(type instanceof GJSONType || (type == null && !(value instanceof Serializable))) // if type == null and incorrect value is passed, value will be not serializable and there will be an exception
            return PValue.getPValue(GwtClientUtils.jsonStringify(value));
        if (type instanceof GADateType)
            return ((GADateType) type).fromJsDate((JsDate)value);

        return PValue.getPValue(toString(value));
    }

    // JS-VALUE STRATEGY SEAM (V1 server-projection; V2 client-typed). Switch point.
    // Returns a PValue, symmetric with convertFromJSValue(type, ...): the form controller's exec/eval/change is
    // type-agnostic (no GType on the client), so for V1 the encoding is type-agnostic canonical
    // (number -> Double, boolean -> Boolean, string -> String, Date -> offset-ISO String yyyy-MM-dd'T'HH:mm:ssXXX,
    // object/array -> JSON String; null/undefined -> null) and the resolved server type projects it. A future V2
    // would swap this body for a type-aware convertFromJSValue(fetchedType, ...) with no change to the callers.
    public static PValue convertFromUnknownJSValue(JavaScriptObject value) {
        if (GwtClientUtils.isUndefinedOrNull(value))
            return null;
        if (isBoolean(value))
            return PValue.getPValue(toBoolean(value));
        if (isNumber(value))
            return PValue.getPValue(toDouble(value));
        if (isDate(value))
            return PValue.getPValue(formatJSDateOffsetISO(value));
        if (isString(value))
            return PValue.getPValue(toString(value));
        return PValue.getPValue(GwtClientUtils.jsonStringify(value)); // object / array
    }
    // PValue -> canonical wire value via convertFileValueBack (DisplayString -> rawString, else unwrap), the same
    // step GClientWebAction.onJSFunctionExecuted uses; for these type-agnostic values it is a plain unwrap today,
    // but keeping it here means a future V2 / new convertFileValueBack semantics land in one place.
    public static Serializable encodeUnknownJSValue(JavaScriptObject value) {
        return PValue.convertFileValueBack(convertFromUnknownJSValue(value));
    }
    public static ArrayList<Serializable> encodeUnknownJSValues(JavaScriptObject array) {
        ArrayList<Serializable> result = new ArrayList<>();
        int length = jsArrayLength(array);
        for (int i = 0; i < length; i++)
            result.add(encodeUnknownJSValue(jsArrayGet(array, i)));
        return result;
    }
    private static native int jsArrayLength(JavaScriptObject array) /*-{ return array.length; }-*/;
    private static native JavaScriptObject jsArrayGet(JavaScriptObject array, int i) /*-{ return array[i]; }-*/;
    private static native boolean isBoolean(JavaScriptObject v) /*-{ return typeof v === 'boolean'; }-*/;
    private static native boolean isNumber(JavaScriptObject v) /*-{ return typeof v === 'number'; }-*/;
    private static native boolean isString(JavaScriptObject v) /*-{ return typeof v === 'string'; }-*/;
    private static native boolean isDate(JavaScriptObject v) /*-{ return Object.prototype.toString.call(v) === '[object Date]'; }-*/;
    // local components + numeric offset (-getTimezoneOffset() so +03:00 stays +03:00; zero -> +00:00, not Z), seconds always
    private static native String formatJSDateOffsetISO(JavaScriptObject d) /*-{
        function p2(n) { return (n < 10 ? '0' : '') + n; }
        var off = -d.getTimezoneOffset();
        var aoff = Math.abs(off);
        return d.getFullYear() + '-' + p2(d.getMonth() + 1) + '-' + p2(d.getDate()) + 'T' +
               p2(d.getHours()) + ':' + p2(d.getMinutes()) + ':' + p2(d.getSeconds()) +
               (off >= 0 ? '+' : '-') + p2(Math.floor(aoff / 60)) + ':' + p2(aoff % 60);
    }-*/;

    public static JavaScriptObject convertToJSValue(GType type, GPropertyDraw property, boolean imageToHTML, PValue value) {
        if (type instanceof GLogicalType) {
            if(!((GLogicalType) type).threeState)
                return fromBoolean(PValue.getBooleanValue(value));

            if(value != null)
                return fromBoolean(PValue.get3SBooleanValue(value));
        }
        if(value == null)
            return null;
        if(type instanceof GIntegralType)
            return fromDouble(((GIntegralType) type).getDoubleValue(value));
        if(property != null && property.isPredefinedImage()) {
            AppBaseImage imageValue = PValue.getImageValue(value);
            if(imageToHTML)
                return fromString(imageValue.createImageHTML());

            return fromObject(imageValue);
        }
        if(type instanceof GImageType)
            return fromString(PValue.getImageValue(value).getImageElementSrc(true)); // assert AppFileImage
        if(type instanceof GFileType)
            return fromString(GwtClientUtils.getAppDownloadURL(PValue.getStringValue(value)));
        if(type instanceof GJSONType)
            return GwtClientUtils.jsonParse(PValue.getCustomStringValue(value));
        if (type instanceof GADateType)
            return ((GADateType) type).toJsDate(value);

        return fromString(PValue.getCustomStringValue(value));
    }
    public static JavaScriptObject convertToJSValue(GPropertyDraw property, PValue value, RendererType rendererType, boolean imageToHTML) {
        return convertToJSValue(property.getRenderType(rendererType), property, imageToHTML, value);
    }

    protected JsArray<JavaScriptObject> getCaptions(NativeHashMap<String, Column> columnMap, BiPredicate<GPropertyDraw, String> filter) {
        JsArray<JavaScriptObject> columns = JavaScriptObject.createArray().cast();
        for (int i = 0, size = properties.size() ; i < size; i++) {
            GPropertyDraw property = properties.get(i);

            List<GGroupObjectValue> propColumnKeys = columnKeys.get(i);
            for (int c = 0; c < propColumnKeys.size(); c++) {
                GGroupObjectValue columnKey = propColumnKeys.get(c);
                if(checkShowIf(i, columnKey))
                    continue;

                String columnName = getColumnSID(property, c, columnKey);
                if (filter != null && !filter.test(property, columnName))
                    continue;

                columnMap.put(columnName, new Column(property, columnKey));
                columns.push(fromString(columnName));
            }
        }
        return columns;
    }


    protected void changeSimpleGroupObject(JavaScriptObject object, boolean rendered, P elementClicked) {
        GGroupObjectValue key = getObjects(object);
        if(key == null && !GwtClientUtils.isUndefinedOrNull(object)) // an EXPLICIT object that didn't resolve (bare key / clone):
            return; // no-op like the form controller, rather than clearing the current object via changeGroupObject(null)

        long requestIndex;
        if(!GwtClientUtils.nullEquals(this.currentKey, key))
            requestIndex = changeGroupObject(key, rendered);
        else
            requestIndex = -2; // we're not waiting for any response, just show popup as it is

        Widget recordView;
        if(elementClicked != null && (recordView = grid.recordView) != null) {
            if(popupObject != null)
                hidePopup();

            popupKey = key;
            popupElementClicked = elementClicked;

            if (recordView.isVisible())
                showPopup();
            else
                popupRequestIndex = requestIndex;
        }
    }

    private void showPopup() {
        popupObject = showPopup(getPopupElement(), popupElementClicked);

        popupRequestIndex = -2; // we are no longer waiting for popup
        popupElementClicked = null; // in theory it's better to do it on popupObject close, but this way is also ok
    }

    protected Element getPopupElement() {
        return grid.recordView.getElement();
    }

    private void hidePopup() {
        hidePopup(popupObject);

        popupObject = null;
        popupKey = null;
    }

    protected abstract JavaScriptObject showPopup(Element popupElement, P popupElementClicked);

    protected abstract void hidePopup(JavaScriptObject popup);

    private JavaScriptObject popupObject;
    private P popupElementClicked = null;
    private GGroupObjectValue popupKey = null;
    private long popupRequestIndex = -2;

    @Override
    public void updateRecordLayout(long requestIndex) {
        Widget recordView = grid.recordView;
        if(popupObject == null) { // no popup
            if(requestIndex <= popupRequestIndex && recordView.isVisible()) // but has to be
                showPopup();
        } else {
            if(!recordView.isVisible())
                hidePopup();
        }
    }

    protected void changeJSProperty(String column, JavaScriptObject object, JavaScriptObject newValue) { // can be UNDEFINED
        changeJSProperties(new String[]{column}, new JavaScriptObject[]{object}, new JavaScriptObject[]{newValue});
    }

    protected void changeJSProperties(String[] columns, JavaScriptObject[] objects, JavaScriptObject[] newValues) {
        PValue[] mappedValues = new PValue[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            Column column = getColumn(columns[i]);
            mappedValues[i] = column != null ? GSimpleStateTableView.convertFromJSUndefValue(column.property, newValues[i]) : null; // unknown column is skipped in changeProperties below
        }
        changeProperties(columns, objects, mappedValues);
    }

    protected void changeProperty(String column, JavaScriptObject object, PValue newValue) {
        changeProperties(new String[]{column}, new JavaScriptObject[]{object}, new PValue[]{newValue});
    }

    protected void changeProperties(String[] columns, JavaScriptObject[] objects, PValue[] newValues) {
        List<GPropertyDraw> properties = new ArrayList<>();
        List<GGroupObjectValue> fullKeys = new ArrayList<>();
        List<PValue> values = new ArrayList<>();

        for (int i = 0; i < columns.length; i++) {
            Column column = getColumn(columns[i]);
            if(column == null) // unknown property key passed from the (external) JS API
                continue;
            GGroupObjectValue objectsKey;
            if(GwtClientUtils.isUndefinedOrNull(objects[i]))
                objectsKey = getSelectedKey(); // null / omitted object -> the current object
            else {
                objectsKey = getObjects(objects[i]); // a data row or a raw objects handle
                if(objectsKey == null) { // an EXPLICIT object that is neither a row nor a handle: reject loudly (mirror the
                    // form controller's controllerChangeProperties) instead of feeding null into getFullKey -> NPE
                    GwtClientUtils.consoleError("changeProperty('" + columns[i] + "'): the object argument is not a data row or an objects handle; pass one of those");
                    continue;
                }
            }
            properties.add(column.property);
            fullKeys.add(GGroupObjectValue.getFullKey(objectsKey, column.columnKey));
            values.add(newValues[i]);
        }

        GPropertyDraw[] propertiesArray = properties.toArray(new GPropertyDraw[0]);
        GGroupObjectValue[] fullKeysArray = fullKeys.toArray(new GGroupObjectValue[0]);
        PValue[] valuesArray = values.toArray(new PValue[0]);

        form.executePropertyEventAction(propertiesArray, fullKeysArray, valuesArray, changeRequestIndex -> {
            for (int i = 0; i < propertiesArray.length; i++) {
                GPropertyDraw property = propertiesArray[i];
                if(valuesArray[i] != PValue.UNDEFINED && property.hasExternalChangeActionForRendering(RendererType.SIMPLE)) { // or use the old value instead of the new value in that case
                    GGroupObjectValue fullKey = fullKeysArray[i];
                    form.pendingChangeProperty(property, fullKey, valuesArray[i], getValue(property, fullKey), changeRequestIndex);
                }
            }
        });
    }

    protected String getCaption(String property) {
        Column column = getColumn(property);
        if(column == null)
            return null;

        GPropertyDraw columnProperty = column.property;
        int propertyIndex = properties.indexOf(columnProperty);
        return GGridTable.getPropertyCaption(captions.get(propertyIndex), columnProperty, column.columnKey);
    }

    protected String getCaptionElementClass(String property) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getCaptionElementClass(column.property, column.columnKey);
    }

    protected boolean isReadOnly(String property, GGroupObjectValue object, boolean rendered) {
        Column column = getColumn(property);
        if(column == null)
            return false;
        return isReadOnly(column.property, object, column.columnKey, rendered);
    }

    protected Boolean isReadOnly(String property, JavaScriptObject object) {
        return isReadOnly(property, getJsObjects(object), true) ? false : null;
    }

    protected String getGridElementClass(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getGridElementClass(column.property, object, column.columnKey);
    }

    protected String getGridElementClass(String property, JavaScriptObject object) {
        return getGridElementClass(property, getJsObjects(object));
    }

    protected String getValueElementClass(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getValueElementClass(column.property, object, column.columnKey);
    }

    protected String getValueElementClass(String property, JavaScriptObject object) {
        return getValueElementClass(property, getJsObjects(object));
    }

    protected GFont getFont(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getFont(column.property, object, column.columnKey);
    }

    protected String getBackground(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getBackground(column.property, object, column.columnKey);
    }

    protected String getPlaceholder(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getPlaceholder(column.property, object, column.columnKey);
    }

    protected String getPattern(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getPattern(column.property, object, column.columnKey);
    }

    protected String getRegexp(String regexp, GGroupObjectValue object) {
        Column column = getColumn(regexp);
        if(column == null)
            return null;
        return getRegexp(column.property, object, column.columnKey);
    }

    protected String getRegexpMessage(String regexpMessage, GGroupObjectValue object) {
        Column column = getColumn(regexpMessage);
        if(column == null)
            return null;
        return getRegexpMessage(column.property, object, column.columnKey);
    }

    protected String getTooltip(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getTooltip(column.property, object, column.columnKey);
    }

    protected String getValueTooltip(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getValueTooltip(column.property, object, column.columnKey);
    }

    protected JavaScriptObject getPropertyCustomOptions(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;

        return convertToJSValue(GJSONType.instance, null, false, getPropertyCustomOptions(column.property, object, column.columnKey));
    }

    protected String getChangeKey(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getChangeKey(column.property, column.columnKey);
    }

    protected String getChangeMouse(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getChangeMouse(column.property, column.columnKey);
    }

    private JavaScriptObject getValue(String property, GGroupObjectValue groupObjectValue) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return convertToJSValue(column.property, getValue(column.property, GGroupObjectValue.getFullKey(groupObjectValue, column.columnKey)), RendererType.SIMPLE, !(this instanceof GMap));
    }

    protected GFont getFont(String property, JavaScriptObject object) {
        return getFont(property, getJsObjects(object));
    }

    protected String getBackground(String property, JavaScriptObject object) {
        return getBackground(property, getJsObjects(object));
    }

    protected String getPlaceholder(String property, JavaScriptObject object) {
        return getPlaceholder(property, getJsObjects(object));
    }

    protected String getPattern(String property, JavaScriptObject object) {
        return getPattern(property, getJsObjects(object));
    }

    protected String getRegexp(String property, JavaScriptObject object) {
        return getRegexp(property, getJsObjects(object));
    }

    protected String getRegexpMessage(String property, JavaScriptObject object) {
        return getRegexpMessage(property, getJsObjects(object));
    }

    protected String getTooltip(String property, JavaScriptObject object) {
        return getTooltip(property, getJsObjects(object));
    }

    protected String getValueTooltip(String property, JavaScriptObject object) {
        return getValueTooltip(property, getJsObjects(object));
    }

    protected JavaScriptObject getPropertyCustomOptions(String property, JavaScriptObject object) {
        return getPropertyCustomOptions(property, getJsObjects(object));
    }

    protected String getChangeKey(String property, JavaScriptObject object) {
        return getChangeKey(property, getJsObjects(object));
    }

    protected String getChangeMouse(String property, JavaScriptObject object) {
        return getChangeMouse(property, getJsObjects(object));
    }

    protected JavaScriptObject getValue(String property, JavaScriptObject object) {
        return getValue(property, getJsObjects(object));
    }

    protected String getForeground(String property, GGroupObjectValue object) {
        Column column = getColumn(property);
        if(column == null)
            return null;
        return getForeground(column.property, object, column.columnKey);
    }

    protected String getForeground(String property, JavaScriptObject object) {
        return getForeground(property, getJsObjects(object));
    }

    protected boolean isCurrentObjectKey(JavaScriptObject object){
        return isCurrentKey(getJsObjects(object));
    }

    // the 2-arg changeProperty(property, X) guess: the column adapter over the single GFormController.isChangeObject
    // core (which form is the call — value or object — decided by the property's value slot)
    public boolean isChangeObject(String property, JavaScriptObject object) {
        Column column = getColumn(property);
        return form.isChangeObject(column != null ? column.property : null, object);
    }

    // the subclass/controller-facing resolution: a row of this form, or an opaque getObjects()/async round-trip raw GGV
    // handle (deployed classic pattern: getObjects(row) kept in JS state, passed back into changeObject/changeProperty
    // later, even after the row left the list). GGroupObjectValue.resolveObject accepts the raw GGV centrally (via
    // fromHandle), so no local instanceof is needed here.
    protected GGroupObjectValue getObjects(JavaScriptObject object) {
        return GGroupObjectValue.resolveObject(object);
    }
    // key can be obtained from getAsyncValues for example, and not passed at all
    protected GGroupObjectValue getJsObjects(JavaScriptObject object) {
        if(GwtClientUtils.isUndefinedOrNull(object)) // raw JS arg: a numeric 0 key would read as null under Java == null (GWT falsy-primitive collapse)
            return getSelectedKey();
        return getObjects(object); // a row of this form or a raw GGV handle; null for anything else (no bare-key/clone resolution)
    }
    protected JavaScriptObject createWithObjects(JavaScriptObject object, JavaScriptObject objects) {
        // a raw GGV handle or a row of this form (getObjects accepts both); anything else (e.g. a bare key) stays unkeyed
        GGroupObjectValue key = getObjects(objects);
        JavaScriptObject created = GwtClientUtils.copyObject(object); // clone the template (registerRow mutates the row below)
        if (key != null)
            GGroupObjectValue.registerRow(created, key); // fabricated rows get the full contract: public key + row-carried `objects` handle
        else
            GGroupObjectValue.clearRowObjects(created); // the clone copied the template's enumerable `objects`; drop it so an unresolvable identity stays unkeyed (resolution reads `objects`, not key)

        return created;
    }

    protected void setDateIntervalViewFilter(String startProperty, String endProperty, int pageSize, JsDate start, JsDate end) {
        Column startColumn = getColumn(startProperty);
        Column endColumn = endProperty != null ? getColumn(endProperty) : startColumn;
        if(startColumn == null || endColumn == null)
            return;

        boolean isDateTimeFilter = !(startColumn.property.getCellType() instanceof GDateType);
        PValue leftBorder = isDateTimeFilter ? GDateTimeType.instance.fromJsDate(start) : GDateType.instance.fromJsDate(start);
        PValue rightBorder = isDateTimeFilter ? GDateTimeType.instance.fromJsDate(end) : GDateType.instance.fromJsDate(end);

        setViewFilters(pageSize, new GPropertyFilter(new GFilter(endColumn.property), grid.groupObject, endColumn.columnKey, leftBorder, GCompare.GREATER_EQUALS),
                new GPropertyFilter(new GFilter(startColumn.property), grid.groupObject, startColumn.columnKey, rightBorder, GCompare.LESS_EQUALS));
    }

    protected void setBooleanViewFilter(String property, int pageSize) {
        Column column = getColumn(property);
        if(column == null)
            return;
        setViewFilters(pageSize, new GPropertyFilter(new GFilter(column.property), grid.groupObject, column.columnKey, PValue.getPValue(true), GCompare.EQUALS));
    }

    private void setViewFilters(int pageSize, GPropertyFilter... filters) {
        form.setViewFilters(Arrays.stream(filters).collect(Collectors.toCollection(ArrayList::new)), pageSize);
        setPageSize(pageSize);
    }

    protected void getAsyncValues(String property, String value, JavaScriptObject successCallBack, JavaScriptObject failureCallBack, int increaseValuesNeededCount, String mode) {
        Column column = getColumn(property);
        if(column == null) { // unknown property key passed from the (external) JS API
            if(failureCallBack != null)
                GwtClientUtils.call(failureCallBack);
            return;
        }
        String actionSID = GFormController.getAsyncActionSID(mode); // default OBJECTS; values/strictValues/change select the server lookup
        if(actionSID == null) { // unknown mode (already logged)
            if(failureCallBack != null)
                GwtClientUtils.call(failureCallBack);
            return;
        }
        form.getAsyncValues(value, column.property, column.columnKey, actionSID, GFormController.getJSCallback(successCallBack, failureCallBack), increaseValuesNeededCount);
    }

    private static class Change<V extends JavaScriptObject> {
        public final String type;
        public final int index;
        public final V object;

        public Change(String type, int index, V object) {
            this.type = type;
            this.index = index;
            this.object = object;
        }

        protected void apply(JavaScriptObject fnc) {
            nativeApply(fnc, type, index, object);
        }
        protected native void nativeApply(JavaScriptObject fnc, String type, int index, V object)/*-{
            fnc(type, index, object)
        }-*/;
    }

    // wagner - fischer algorithm
    // diff from s2 make s1
    private static <V extends JavaScriptObject> ArrayList<Change<V>> buildDiff(JsArray<V> s1, JsArray<V> s2, int insertCost, int deleteCost, BiFunction<V, V, Integer> fnReplaceCost, boolean removeFirst) {
        ArrayList<Change<V>> diff = new ArrayList<>();

        int c1 = s1.length();
        int c2 = s2.length();

        int[][] D = new int[c2+1][];
        for(int i1 = 0; i1 <= c2; i1++)
            D[i1] = new int[c1+1];

        D[0][0]=0;
        for(int i1 = 1; i1 <= c1; i1++)
            D[0][i1] = D[0][i1-1] + insertCost;
        for(int i2 = 1; i2 <= c2; i2++) {
            D[i2][0] = D[i2 - 1][0] + deleteCost;
            for (int i1 = 1; i1 <= c1; i1++) {
                int replaceCost = fnReplaceCost.apply(s1.get(i1 - 1), s2.get(i2 - 1));
                D[i2][i1] = Math.min(Math.min(D[i2 - 1][i1] + deleteCost,
                        D[i2][i1 - 1] + insertCost),
                        D[i2 - 1][i1 - 1] + replaceCost);
            }
        }

        int i2 = c2;
        int i1 = c1;
        while(i2 > 0 || i1 > 0) {
            int cost = D[i2][i1];
            if(i1 == 0 || (i2 > 0 && cost == D[i2 - 1][i1] + deleteCost)) {
                i2 = i2 - 1;
                diff.add(new Change<>("remove", i1, s2.get(i2)));
            } else if (i2 == 0 || cost == D[i2][i1 - 1] + insertCost) {
                i1 = i1 - 1;
                diff.add(new Change<>("add", i1, s1.get(i1)));
            } else {
                i2 = i2 - 1;
                i1 = i1 - 1;

                if(D[i2 + 1][i1 + 1] - D[i2][i1] > 0)
                    diff.add(new Change<>("update", i1, s1.get(i1)));
            }
        }

        Collections.reverse(diff);
        return removeFirst ? removeFirstOrder(diff) : diff;
    }

    private static <V extends JavaScriptObject> ArrayList<Change<V>> removeFirstOrder(ArrayList<Change<V>> diff) {
        ArrayList<Change<V>> removeFirstDiff = new ArrayList<>();
        int addCount = 0;
        for (Change<V> ch : diff) {
            if (ch.type.equals("remove")) {
                removeFirstDiff.add(new Change<>(ch.type, ch.index - addCount, ch.object));
            } else if (ch.type.equals("add")) {
                ++addCount;
            }
        }
        for (Change<V> ch : diff) {
            if (!ch.type.equals("remove")) {
                removeFirstDiff.add(ch);
            }
        }
        return removeFirstDiff;
    }

    // fnc - method with params:
    //  type - remove, add, update
    //  index - current location
    //  object - object

    // replaceFnc (to, from) - 0 if equals, so no replace should be done, 100000 if can not be updated, 1 - if can be updated

    // option - 0 if totally equals
    // 100000 if key are not equals
    // 1 - otherwise

    public static <K, V extends JavaScriptObject> void clearDiff(Element element) {
        GwtClientUtils.removeField(element, "prevList");
    }

    public static <K, V extends JavaScriptObject> void diff(JsArray<V> list, Element element, JavaScriptObject proceed, JavaScriptObject getObjectString, String objectsField, boolean noDiffObjects, boolean removeFirst) {
        JsArray<V> prevList = (JsArray<V>) GwtClientUtils.getField(element, "prevList");
        if(prevList == null)
            prevList = GwtClientUtils.emptyArray();

        ArrayList<Change<V>> diff = buildDiff(list, prevList, 10, 10, (to, from) -> {
                                                    if(!GSimpleStateTableView.toString(GwtClientUtils.call(getObjectString, to)).equals(GSimpleStateTableView.toString(GwtClientUtils.call(getObjectString, from))))
                                                        return noDiffObjects ? 100000 : 1;
                                                    return GwtClientUtils.plainEquals(to, from, objectsField) ? 0 : 1;
                                                }, removeFirst);
        for (Change<V> vChange : diff) {
            vChange.apply(proceed);
        }

        GwtClientUtils.setField(element, "prevList", list);
    }

    public static <V extends JavaScriptObject> JsArray<V> changeJSDiff(Element element, JsArray<V> list, V object, JavaScriptObject controller, String propertyName, JavaScriptObject newValue, String type, int index) {
        JsArray<V> prevList = (JsArray<V>) GwtClientUtils.getField(element, "prevList");
        if(list == null) {
            if (prevList == null)
                return null;
            list = prevList;
        } else {
            // assert prevList == null || list.equals(prevList)
        }

        list = changeDiff(list, object, controller, propertyName, newValue, type, index);

        if(prevList != null)
            GwtClientUtils.setField(element, "prevList", list);

        return list;
    }

    public static native <V extends JavaScriptObject> JsArray<V> changeDiff(JsArray<V> list, V object, JavaScriptObject controller, String propertyName, JavaScriptObject newValue, String type, int index)/*-{
        if(type === "add")
            return $wnd.addObjectToArray(list, $wnd.replaceField(object, propertyName, newValue), index == null ? list.length : index);

        var objectsString = controller.getObjectsString(object);
        var testFunction = function (oldObject) { return controller.getObjectsString(oldObject) === objectsString; };

        if(type === "remove")
            return $wnd.removeObjectFromArray(list, testFunction);

        return $wnd.replaceObjectFieldInArray(list, testFunction, propertyName, newValue);
    }-*/;

    protected native JavaScriptObject getController(Element element, JavaScriptObject formController)/*-{
        var thisObj = this;
        return {
            changeProperty: function (property, object, newValue, type, index) {
                if (!thisObj.@GSimpleStateTableView::isGridProperty(Ljava/lang/String;)(property))
                    // not a column of THIS grid (e.g. a PANEL action edit/delete): the form controller's job (#1655) -
                    // forward to it; it resolves the property form-wide (any group, incl. form-level) and guesses/execs
                    return formController.changeProperty(property, object, newValue);
                if(object !== undefined) {
                    if(newValue === undefined) { //object passed, newValue not passed
                        //guess if object is object or newValue
                        if (thisObj.@GSimpleStateTableView::isChangeObject(*)(property, object)) {
                            newValue = @GwtClientUtils::UNDEFINED;
                        } else {
                            newValue = object;
                            object = null;
                        }
                    }
                } else {
                    if(newValue === undefined)
                        newValue = @GwtClientUtils::UNDEFINED;
                    object = null;
                }

                @GSimpleStateTableView::changeJSDiff(*)(element, null, object, this, property, newValue, type, index);

                thisObj.@GSimpleStateTableView::changeJSProperty(*)(property, object, newValue);
            },
            //todo deprecated. use changeProperty instead
            changeDateProperty: function (property, object, year, month, day) {
                return changeProperty(property, object, new Date(year, month - 1, day))
            },
            changeProperties: function (properties, objects, newValues) {
                return thisObj.@GSimpleStateTableView::changeJSProperties(*)(properties, objects, newValues);
            },
            isCurrent: function (object) {
                return thisObj.@GSimpleStateTableView::isCurrentObjectKey(*)(object);
            },
            getCaption: function (property) {
                return thisObj.@GSimpleStateTableView::getCaption(Ljava/lang/String;)(property);
            },
            getCaptionClass: function (property) {
                return thisObj.@GSimpleStateTableView::getCaptionElementClass(Ljava/lang/String;)(property);
            },
            isPropertyReadOnly: function (property, object) {
                return thisObj.@GSimpleStateTableView::isReadOnly(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            isTabFocusable: function () {
                return true;
            },
            getGridClass: function (property, object) {
                return thisObj.@GSimpleStateTableView::getGridElementClass(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getValueClass: function (property, object) {
                return thisObj.@GSimpleStateTableView::getValueElementClass(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getFont: function (property, object) {
                return thisObj.@GSimpleStateTableView::getFont(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getBackground: function (property, object) {
                return thisObj.@GSimpleStateTableView::getBackground(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getForeground: function (property, object) {
                return thisObj.@GSimpleStateTableView::getForeground(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getPlaceholder: function (property, object) {
                return thisObj.@GSimpleStateTableView::getPlaceholder(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getPattern: function (property, object) {
                return thisObj.@GSimpleStateTableView::getPattern(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getRegexp: function (property, object) {
                return thisObj.@GSimpleStateTableView::getRegexp(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getRegexpMessage: function (property, object) {
                return thisObj.@GSimpleStateTableView::getRegexpMessage(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getTooltip: function (property, object) {
                return thisObj.@GSimpleStateTableView::getTooltip(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getValueTooltip: function (property, object) {
                return thisObj.@GSimpleStateTableView::getValueTooltip(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getPropertyCustomOptions: function (property, object) {
                return thisObj.@GSimpleStateTableView::getPropertyCustomOptions(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getChangeKey: function (property, object) {
                return thisObj.@GSimpleStateTableView::getChangeKey(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getChangeMouse: function (property, object) {
                return thisObj.@GSimpleStateTableView::getChangeMouse(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getValue: function (property, object) {
                return thisObj.@GSimpleStateTableView::getValue(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            changeObject: function (object, rendered, elementClicked) {
                if(rendered === undefined)
                    rendered = false;
                if(elementClicked === undefined)
                    elementClicked = null;
                return thisObj.@GSimpleStateTableView::changeSimpleGroupObject(*)(object, rendered, elementClicked);
            },
            setDateIntervalViewFilter: function (startProperty, endProperty, pageSize, start, end) {
                thisObj.@GSimpleStateTableView::setDateIntervalViewFilter(*)(startProperty, endProperty, pageSize, start, end);
            },
            setBooleanViewFilter: function (property, pageSize) {
                thisObj.@GSimpleStateTableView::setBooleanViewFilter(*)(property, pageSize);
            },
            getGroupObjectBackgroundColor: function(object) {
                var color = object.color;
                if (color)
                    return color.toString();
                return thisObj.@GStateTableView::getRowBackgroundColor(*)(thisObj.@GSimpleStateTableView::getJsObjects(*)(object));
            },
            getDisplayBackgroundColor: function (color, isCurrent) {
                return @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(color);
            },
            getDisplayForegroundColor: function (color) {
                return @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(color);
            },
            getGroupObjectForegroundColor: function(object) {
                return thisObj.@GStateTableView::getRowForegroundColor(*)(thisObj.@GSimpleStateTableView::getJsObjects(*)(object));
            },
            getValues: function (property, value, successCallback, failureCallback) {
                return this.getPropertyValues(property, value, successCallback, failureCallback);
            },
            // getPropertyValues(property, value[, mode], ok, fail[, count]); mode right after value (parameter-guess: ok = first
            // function arg, mode present iff there are 2 pre-callback args). mode: 'objects' (def, item.objects = handle) | 'values' | 'change'
            getPropertyValues: function (property) {
                var okIndex = -1;
                for (var i = 1; i < arguments.length; i++)
                    if (typeof arguments[i] === 'function') { okIndex = i; break; }
                var value = arguments[1];
                var mode = okIndex === 3 ? arguments[2] : null; // {value, mode, ok, ...} vs {value, ok, ...}
                var successCallback = arguments[okIndex], failureCallback = arguments[okIndex + 1], count = arguments[okIndex + 2];
                return thisObj.@GSimpleStateTableView::getAsyncValues(*)(property, value, successCallback, failureCallback, count == null ? 0 : count, mode == null ? null : mode);
            },
            getObjects: function (object) {
                return thisObj.@GSimpleStateTableView::getObjects(*)(object);
            },
            getObjectsField: function () {
                return @lsfusion.gwt.client.form.object.GGroupObjectValue::ROW_OBJECTS;
            },
            getObjectsString: function (object) {
                // the canonical key string (shared with the CUSTOM REACT contract); used internally for diff EQUALITY
                // only — null-tolerant: a clone of a since-deleted/fabricated row may no longer resolve to a handle,
                // but its copied public key is still a perfectly good equality token
                var k = this.getObjects(object);
                if (k !== null) return k.@lsfusion.gwt.client.form.object.GGroupObjectValue::toKeyString()();
                return (object !== null && typeof object === 'object') ? String(object.key) : String(object);
            },
            createObject: function (object, objects) {
                return thisObj.@GSimpleStateTableView::createWithObjects(*)(object, objects);
            },
            diff: function (newList, fnc, noDiffObjects, removeFirst) {
                var controller = this;
                @GSimpleStateTableView::diff(*)(newList, element, fnc, function(object) {return controller.getObjectsString(object);}, controller.getObjectsField(), noDiffObjects, removeFirst); // exclude the `objects` identity handle from content diff (it's identity, not content; === stability is only a twins-cache optimization)
            },
            clearDiff: function () {
                @GSimpleStateTableView::clearDiff(*)(element);
            },
            getColorThemeName: function () {
                return @lsfusion.gwt.client.view.MainFrame::colorTheme.@java.lang.Enum::name()();
            },
            isList: function () {
                return true;
            },
            isRenderInputKeyEvent: function (event, multiLine) {
                return false;
            },
            isEditInputKeyEvent: function (event, multiLine) {
                return false;
            },
            previewEvent: function (element, event) {
                return thisObj.@GSimpleStateTableView::previewEvent(*)(element, event);
            },
            form: formController
        };
    }-*/;

    protected static String getCaption(JavaScriptObject element, Function<JavaScriptObject, String> defaultCaption) {
        String elementCaption = getElementCaption(element);
        return elementCaption != null ? elementCaption : defaultCaption.apply(element);
    }

    protected static BaseImage getImage(JavaScriptObject element, Supplier<BaseImage> defaultImage) {
        BaseImage elementImage = getElementImage(element);
        return elementImage != null ? elementImage : defaultImage.get();
    }

    private static native String getElementCaption(JavaScriptObject element)/*-{
        return element.caption;
    }-*/;

    // icon - deprecated
    private static native BaseImage getElementImage(JavaScriptObject element)/*-{
        return element.image ? element.image : (element.icon ? @lsfusion.gwt.client.base.AppLinkImage::new(Ljava/lang/String;Ljava/lang/String;)(element.icon, "") : null);
    }-*/;

    protected Element createImageCaptionElement(BaseImage image, String caption, ImageHtmlOrTextType type) {
        Element element = GwtClientUtils.createFocusElement("div");
        BaseImage.initImageText(element, type);
        BaseImage.updateText(element, caption);
        BaseImage.updateImage(image, element);
        return element;
    }

    protected boolean previewEvent(Element element, Event event) {
        return form.previewCustomEvent(event, element);
    }
}
