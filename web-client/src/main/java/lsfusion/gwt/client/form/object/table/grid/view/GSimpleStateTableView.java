package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GAsync;
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

    private Column getColumn(String key) {
        Column column = columnMap.get(key);
        if (column == null) {
            for (GPropertyDraw property : form.getPropertyDraws()) {
                if (key.equals(property.integrationSID)) {
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
        if (type instanceof GLogicalType) {
            if(!((GLogicalType) type).threeState)
                return PValue.getPValue(toBoolean(value));

            return PValue.getPValue(value != null ? toBoolean(value) : null);
        }
        if(value == null)
            return null;
        if(type instanceof GIntegralType)
            return ((GIntegralType) type).fromDoubleValue(toDouble(value));
        if(type instanceof GJSONType || (type == null && !(value instanceof Serializable))) // if type == null and incorrect value is passed, value will be not serializable and there will be an exception
            return PValue.getPValue(GwtClientUtils.jsonStringify(value));
        if (type instanceof GADateType)
            return ((GADateType) type).fromJsDate((JsDate)value);

        return PValue.getPValue(toString(value));
    }

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
    private static JavaScriptObject convertToJSKey(Serializable key) {
        if(key instanceof GGroupObjectValue)
            return fromObject(key);

        return GwtClientUtils.jsonParse((String)key);
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
        if (filter == null)
            columns.push(fromString(objectsFieldName));
        return columns;
    }

    protected final static String objectsFieldName = "#__key";

    protected void changeSimpleGroupObject(JavaScriptObject object, boolean rendered, P elementClicked) {
        GGroupObjectValue key = getObjects(object);

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
        for (int i = 0; i < newValues.length; i++)
            mappedValues[i] = GSimpleStateTableView.convertFromJSUndefValue(getColumn(columns[i]).property, newValues[i]);
        changeProperties(columns, objects, mappedValues);
    }

    protected void changeProperty(String column, JavaScriptObject object, PValue newValue) {
        changeProperties(new String[]{column}, new JavaScriptObject[]{object}, new PValue[]{newValue});
    }

    protected void changeProperties(String[] columns, JavaScriptObject[] objects, PValue[] newValues) {
        int length = columns.length;
        GPropertyDraw[] properties = new GPropertyDraw[length];
        GGroupObjectValue[] fullKeys = new GGroupObjectValue[length];

        for (int i = 0; i < length; i++) {
            Column column = getColumn(columns[i]);
            properties[i] = column.property;
            fullKeys[i] = GGroupObjectValue.getFullKey(getJsObjects(objects[i]), column.columnKey);
        }

        form.executePropertyEventAction(properties, fullKeys, newValues, changeRequestIndex -> {
            for (int i = 0; i < length; i++) {
                GPropertyDraw property = properties[i];
                if(newValues[i] != PValue.UNDEFINED && property.hasExternalChangeActionForRendering(RendererType.SIMPLE)) { // or use the old value instead of the new value in that case
                    GGroupObjectValue fullKey = fullKeys[i];
                    form.pendingChangeProperty(property, fullKey, newValues[i], getValue(property, fullKey), changeRequestIndex);
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

    public static boolean isChangeObject(JavaScriptObject object) {
        return hasKey(object, objectsFieldName) || toObject(object) instanceof GGroupObjectValue;
    }

    // key can be obtained from getAsyncValues for example, and not passed at all
    protected GGroupObjectValue getJsObjects(JavaScriptObject object) {
        if(object == null)
            return getSelectedKey();
        if(hasKey(object, objectsFieldName))
            object = getValue(object, objectsFieldName);
        return toObject(object);
    }
    protected GGroupObjectValue getObjects(JavaScriptObject object) {
        return toObject(getValue(object, objectsFieldName));
    }
    protected JavaScriptObject createWithObjects(JavaScriptObject object, GGroupObjectValue key) {
        return GwtClientUtils.replaceField(object, objectsFieldName, fromObject(key));
    }
    protected String getObjectsField() {
        return objectsFieldName;
    }

    protected void setDateIntervalViewFilter(String startProperty, String endProperty, int pageSize, JsDate start, JsDate end, boolean isDateTimeFilter) {

        PValue leftBorder = isDateTimeFilter ? GDateTimeType.instance.fromJsDate(start) : GDateType.instance.fromJsDate(start);
        PValue rightBorder = isDateTimeFilter ? GDateTimeType.instance.fromJsDate(end) : GDateType.instance.fromJsDate(end);

        Column startColumn = getColumn(startProperty);
        Column endColumn = endProperty != null ? getColumn(endProperty) : startColumn;

        setViewFilters(pageSize, new GPropertyFilter(new GFilter(endColumn.property), grid.groupObject, endColumn.columnKey, leftBorder, GCompare.GREATER_EQUALS),
                new GPropertyFilter(new GFilter(startColumn.property), grid.groupObject, startColumn.columnKey, rightBorder, GCompare.LESS_EQUALS));
    }

    protected void setBooleanViewFilter(String property, int pageSize) {
        Column column = getColumn(property);
        setViewFilters(pageSize, new GPropertyFilter(new GFilter(column.property), grid.groupObject, column.columnKey, PValue.getPValue(true), GCompare.EQUALS));
    }

    private void setViewFilters(int pageSize, GPropertyFilter... filters) {
        form.setViewFilters(Arrays.stream(filters).collect(Collectors.toCollection(ArrayList::new)), pageSize);
        setPageSize(pageSize);
    }

    protected void getAsyncValues(String property, String value, JavaScriptObject successCallBack, JavaScriptObject failureCallBack, int increaseValuesNeededCount) {
        Column column = getColumn(property);
        form.getAsyncValues(value, column.property, column.columnKey, ServerResponse.OBJECTS, getJSCallback(successCallBack, failureCallBack), increaseValuesNeededCount);
    }

    public static AsyncCallback<GFormController.GAsyncResult> getJSCallback(JavaScriptObject successCallBack, JavaScriptObject failureCallBack) {
        return new AsyncCallback<GFormController.GAsyncResult>() {
            @Override
            public void onFailure(Throwable caught) {
                if (failureCallBack != null)
                    GwtClientUtils.call(failureCallBack);
            }

            @Override
            public void onSuccess(GFormController.GAsyncResult result) {
                assert !result.needMoreSymbols;
                ArrayList<GAsync> asyncs = result.asyncs;
                if (asyncs == null) {
                    if (!result.moreRequests && failureCallBack != null)
                        GwtClientUtils.call(failureCallBack);
                    return;
                }

                GwtClientUtils.call(successCallBack, convertToJSObject(result));
            }
        };
    }

    private static JavaScriptObject convertToJSObject(GFormController.GAsyncResult result) {
        JavaScriptObject[] results = new JavaScriptObject[result.asyncs.size()];
        for (int i = 0; i < result.asyncs.size(); i++) {
            JavaScriptObject object = GwtClientUtils.newObject();
            GAsync suggestion = result.asyncs.get(i);
            GwtClientUtils.setField(object, "displayString", fromString(PValue.getStringValue(suggestion.getDisplayValue())));
            GwtClientUtils.setField(object, "rawString", fromString(PValue.getStringValue(suggestion.getRawValue())));
            GwtClientUtils.setField(object, "objects", convertToJSKey(suggestion.key));
            results[i] = object;
        }
        JavaScriptObject data = GwtClientUtils.newObject();
        GwtClientUtils.setField(data, "data", fromObject(results));
        GwtClientUtils.setField(data, "more", fromBoolean(result.moreRequests));
        return data;
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
                if(object !== undefined) {
                    if(newValue === undefined) { //object passed, newValue not passed
                        //guess if object is object or newValue
                        if (@GSimpleStateTableView::isChangeObject(*)(object)) {
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
            setDateIntervalViewFilter: function (startProperty, endProperty, pageSize, start, end, isDateTimeFilter) {
                thisObj.@GSimpleStateTableView::setDateIntervalViewFilter(*)(startProperty, endProperty, pageSize, start, end, isDateTimeFilter);
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
            getPropertyValues: function (property, value, successCallback, failureCallback, increaseValuesNeededCount) {
                return thisObj.@GSimpleStateTableView::getAsyncValues(*)(property, value, successCallback, failureCallback, increaseValuesNeededCount != null ? increaseValuesNeededCount : 0);
            },
            getObjects: function (object) {
                return thisObj.@GSimpleStateTableView::getObjects(*)(object);
            },
            getObjectsField: function () {
                return thisObj.@GSimpleStateTableView::getObjectsField(*)();
            },
            getObjectsString: function (object) {
                return this.getObjects(object).toString();
            },
            createObject: function (object, objects) {
                return thisObj.@GSimpleStateTableView::createWithObjects(*)(object, objects);
            },
            diff: function (newList, fnc, noDiffObjects, removeFirst) {
                var controller = this;
                @GSimpleStateTableView::diff(*)(newList, element, fnc, function(object) {return controller.getObjectsString(object);}, this.getObjectsField(), noDiffObjects, removeFirst);
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
