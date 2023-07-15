package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GAsync;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GPushAsyncInput;
import lsfusion.gwt.client.form.property.async.GPushAsyncResult;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.interop.action.ServerResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static lsfusion.gwt.client.base.view.grid.DataGrid.initSinkEvents;

public abstract class GSimpleStateTableView<P> extends GStateTableView {

    protected final JavaScriptObject controller;
    private final TableContainer tableContainer;

    public GSimpleStateTableView(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(form, grid);

        this.controller = getController();
        this.tableContainer = tableContainer;
        GwtClientUtils.setZeroZIndex(getDrawElement());

        initSinkEvents(this);
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = DataGrid.getTargetAndCheck(getElement(), event);
        if(target == null || (popupObject != null && getPopupElement().isOrHasChild(target))) // if there is a popupElement we'll consider it not to be part of this view (otherwise on mouse change event focusElement.focus works, and popup panel elements looses focus)
            return;
        if(!form.previewEvent(target, event))
            return;

        super.onBrowserEvent(event);

        if(!DataGrid.checkSinkEvents(event))
            return;

        Element cellParent = getCellParent(target);
        form.onPropertyBrowserEvent(new EventHandler(event), cellParent, cellParent != null, getTableDataFocusElement(),
                handler -> {}, // no outer context
                handler -> {}, // no edit
                handler -> {}, // no outer context
                handler -> {}, handler -> {}, // no copy / paste for now
                false, true
        );
    }

    public Element getTableDataFocusElement() {
        return tableContainer.getElement();
    }

    protected abstract Element getCellParent(Element target);

    private NativeHashMap<String, Column> columnMap;

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

                rowValues.push(convertToJSValue(property, propValues.get(fullKey)));
            }
        }
        rowValues.push(fromObject(key));
        return rowValues;
    }
    public static PValue convertFromJSUndefValue(GType type, JavaScriptObject value) {
        if(GwtClientUtils.isString(value, UNDEFINED))
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
            return ((GIntegralType) type).convertDouble(toDouble(value));
        if(type instanceof GJSONType)
            return PValue.getPValue(GwtClientUtils.jsonStringify(value));

        return PValue.getPValue(toString(value));
    }
    public static JavaScriptObject convertToJSValue(GType type, PValue value) {
        if (type instanceof GLogicalType) {
            if(!((GLogicalType) type).threeState)
                return fromBoolean(PValue.getBooleanValue(value));

            if(value != null)
                return fromBoolean(PValue.get3SBooleanValue(value));
        }
        if(value == null)
            return null;
        if(type instanceof GIntegralType)
            return fromDouble((PValue.getNumberValue(value)).doubleValue());
        if(type instanceof GImageType)
            return fromString(GwtClientUtils.getAppDownloadURL(PValue.getCustomStringValue(value)));
        if(type instanceof GJSONType)
            return GwtClientUtils.jsonParse(PValue.getCustomStringValue(value));

        return fromString(PValue.getCustomStringValue(value));
    }

    public static JavaScriptObject convertToJSValue(GPropertyDraw property, PValue value) {
        return convertToJSValue(property.baseType, value);
    }

    protected JsArray<JavaScriptObject> getCaptions(NativeHashMap<String, Column> columnMap, Predicate<GPropertyDraw> filter) {
        JsArray<JavaScriptObject> columns = JavaScriptObject.createArray().cast();
        for (int i = 0, size = properties.size() ; i < size; i++) {
            GPropertyDraw property = properties.get(i);
            if (filter!= null && !filter.test(property))
                continue;

            List<GGroupObjectValue> propColumnKeys = columnKeys.get(i);
            for (int c = 0; c < propColumnKeys.size(); c++) {
                GGroupObjectValue columnKey = propColumnKeys.get(c);
                if(checkShowIf(i, columnKey))
                    continue;

                String columnName = getColumnSID(property, c, columnKey);
                columnMap.put(columnName, new Column(property, columnKey));
                columns.push(fromString(columnName));
            }
        }
        if (filter == null)
            columns.push(fromString(keysFieldName));
        return columns;
    }

    protected final static String keysFieldName = "#__key";

    protected void changeSimpleGroupObject(JavaScriptObject object, boolean rendered, P elementClicked) {
        GGroupObjectValue key = getKey(object);

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
        popupObject = showPopup(popupElementClicked, getPopupElement());

        popupRequestIndex = -2; // we are no longer waiting for popup
        popupElementClicked = null; // in theory it's better to do it on popupObject close, but this way is also ok
    }

    private Element getPopupElement() {
        return grid.recordView.getElement();
    }

    private void hidePopup() {
        hidePopup(popupObject);

        popupObject = null;
        popupKey = null;
    }

    protected abstract JavaScriptObject showPopup(P popupElementClicked, Element popupElement);

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
            mappedValues[i] = GSimpleStateTableView.convertFromJSUndefValue(columnMap.get(columns[i]).property.getExternalChangeType(), newValues[i]);
        changeProperties(columns, objects, mappedValues);
    }

    protected void changeProperty(String column, JavaScriptObject object, PValue newValue) {
        changeProperties(new String[]{column}, new JavaScriptObject[]{object}, new PValue[]{newValue});
    }

    public static final String UNDEFINED = "undefined";

    protected void changeProperties(String[] columns, JavaScriptObject[] objects, PValue[] newValues) {
        int length = columns.length;
        GPropertyDraw[] properties = new GPropertyDraw[length];
        GGroupObjectValue[] fullKeys = new GGroupObjectValue[length];
        boolean[] externalChanges = new boolean[length];
        GPushAsyncResult[] pushAsyncResults = new GPushAsyncResult[length];

        for (int i = 0; i < length; i++) {
            Column column = columnMap.get(columns[i]);
            properties[i] = column.property;
            fullKeys[i] = GGroupObjectValue.getFullKey(getChangeKey(objects[i]), column.columnKey);
            externalChanges[i] = true;
            PValue newValue = newValues[i];
            pushAsyncResults[i] = newValue == PValue.UNDEFINED ? null : new GPushAsyncInput(new GUserInputResult(newValue));
        }

        Consumer<Long> onExec = changeRequestIndex -> {
            for (int i = 0; i < length; i++) {
                GPropertyDraw property = properties[i];
                if(newValues[i] != PValue.UNDEFINED && property.canUseChangeValueForRendering(property.getExternalChangeType())) { // or use the old value instead of the new value in that case
                    GGroupObjectValue fullKey = fullKeys[i];
                    form.pendingChangeProperty(property, fullKey, newValues[i], getValue(property, fullKey), changeRequestIndex);
                }
            }
        };
        String actionSID = GEditBindingMap.CHANGE;
        if(length == 1 && newValues[0] == PValue.UNDEFINED)
            form.executePropertyEventAction(properties[0], fullKeys[0], actionSID, (GPushAsyncInput) pushAsyncResults[0], externalChanges[0], onExec);
        else
            onExec.accept(form.asyncExecutePropertyEventAction(actionSID, null, null, properties, externalChanges, fullKeys, pushAsyncResults));
    }

    protected String getCaption(String property) {
        Column column = columnMap.get(property);
        if(column == null)
            return null;

        GPropertyDraw columnProperty = column.property;
        int propertyIndex = properties.indexOf(columnProperty);
        return GGridTable.getPropertyCaption(captions.get(propertyIndex), columnProperty, column.columnKey);
    }

    protected String getCaptionElementClass(String property) {
        Column column = columnMap.get(property);
        if(column == null)
            return null;
        return getCaptionElementClass(column.property, column.columnKey);
    }

    protected boolean isReadOnly(String property, GGroupObjectValue object) {
        Column column = columnMap.get(property);
        if(column == null)
            return false;
        return isReadOnly(column.property, object, column.columnKey);
    }

    protected boolean isReadOnly(String property, JavaScriptObject object) {
        return isReadOnly(property, getKey(object));
    }

    protected String getValueElementClass(String property, GGroupObjectValue object) {
        Column column = columnMap.get(property);
        if(column == null)
            return null;
        return getValueElementClass(column.property, object, column.columnKey);
    }

    protected String getValueElementClass(String property, JavaScriptObject object) {
        return getValueElementClass(property, getKey(object));
    }

    protected String getBackground(String property, GGroupObjectValue object) {
        Column column = columnMap.get(property);
        if(column == null)
            return null;
        return getBackground(column.property, object, column.columnKey);
    }

    protected String getBackground(String property, JavaScriptObject object) {
        return getBackground(property, getKey(object));
    }

    protected String getForeground(String property, GGroupObjectValue object) {
        Column column = columnMap.get(property);
        if(column == null)
            return null;
        return getForeground(column.property, object, column.columnKey);
    }

    protected String getForeground(String property, JavaScriptObject object) {
        return getForeground(property, getKey(object));
    }

    protected boolean isCurrentObjectKey(JavaScriptObject object){
        return isCurrentKey(getKey(object));
    }

    // change key can be outside view window, and can be obtained from getAsyncValues for example
    protected GGroupObjectValue getChangeKey(JavaScriptObject object) {
        if(object == null)
            return getSelectedKey();
        if(hasKey(object, keysFieldName))
            object = getValue(object, keysFieldName);
        return toObject(object);
    }
    protected GGroupObjectValue getKey(JavaScriptObject object) {
        return toObject(getValue(object, keysFieldName));
    }

    protected void changeDateTimeProperty(String property, JavaScriptObject object, int year, int month, int day, int hour, int minute, int second) {
        changeProperty(property, object, PValue.getPValue(new GDateTimeDTO(year, month, day, hour, minute, second)));
    }

    protected void changeDateProperty(String property, JavaScriptObject object, int year, int month, int day) {
        changeProperty(property, object, PValue.getPValue(new GDateDTO(year, month, day)));
    }

    protected void changeDateTimeProperties(String[] properties, JavaScriptObject[] objects, int[] years, int[] months, int[] days, int[] hours, int[] minutes, int[] seconds) {
        int length = objects.length;
        PValue[] gDateTimeDTOs = new PValue[length];
        for (int i = 0; i < length; i++) {
            gDateTimeDTOs[i] = PValue.getPValue(new GDateTimeDTO(years[i], months[i], days[i], hours[i], minutes[i], seconds[i]));
        }
        changeProperties(properties, objects, gDateTimeDTOs);
    }

    protected void changeDateProperties(String[] properties, JavaScriptObject[] objects, int[] years, int[] months, int[] days) {
        int length = objects.length;
        PValue[] gDateDTOs = new PValue[length];
        for (int i = 0; i < length; i++) {
            gDateDTOs[i] = PValue.getPValue(new GDateDTO(years[i], months[i], days[i]));
        }
        changeProperties(properties, objects, gDateDTOs);
    }

    protected void setDateIntervalViewFilter(String property, int pageSize, int startYear, int startMonth, int startDay, int endYear, int endMonth, int endDay, boolean isDateTimeFilter) {
        PValue leftBorder = isDateTimeFilter ? PValue.getPValue(new GDateTimeDTO(startYear, startMonth, startDay, 0, 0, 0)) : PValue.getPValue(new GDateDTO(startYear, startMonth, startDay)) ;
        PValue rightBorder = isDateTimeFilter ? PValue.getPValue(new GDateTimeDTO(endYear, endMonth, endDay, 0, 0, 0)) : PValue.getPValue(new GDateDTO(endYear, endMonth, endDay));

        Column column = columnMap.get(property);
        setViewFilters(pageSize, new GPropertyFilter(new GFilter(column.property), grid.groupObject, column.columnKey, leftBorder, GCompare.GREATER_EQUALS),
                new GPropertyFilter(new GFilter(column.property), grid.groupObject, column.columnKey, rightBorder, GCompare.LESS_EQUALS));
    }

    protected void setBooleanViewFilter(String property, int pageSize) {
        Column column = columnMap.get(property);
        setViewFilters(pageSize, new GPropertyFilter(new GFilter(column.property), grid.groupObject, column.columnKey, PValue.getPValue(true), GCompare.EQUALS));
    }

    private void setViewFilters(int pageSize, GPropertyFilter... filters) {
        form.setViewFilters(Arrays.stream(filters).collect(Collectors.toCollection(ArrayList::new)), pageSize);
        setPageSize(pageSize);
    }

    private String getPropertyJsValue(String property) {
        Column column = columnMap.get(property);
        Object value = column != null ? getJsValue(column.property, getSelectedKey(), column.columnKey) : null;
        return value != null ? value.toString() : null;
    }

    protected void getAsyncValues(String property, String value, JavaScriptObject successCallBack, JavaScriptObject failureCallBack) {
        Column column = columnMap.get(property);
        form.getAsyncValues(value, column.property, column.columnKey, ServerResponse.OBJECTS, new AsyncCallback<GFormController.GAsyncResult>() {
            @Override
            public void onFailure(Throwable caught) {
                if(failureCallBack != null)
                    GwtClientUtils.call(failureCallBack);
            }

            @Override
            public void onSuccess(GFormController.GAsyncResult result) {
                assert !result.needMoreSymbols;
                ArrayList<GAsync> asyncs = result.asyncs;
                if(asyncs == null) {
                    if(!result.moreRequests && failureCallBack != null)
                        GwtClientUtils.call(failureCallBack);
                    return;
                }

                JavaScriptObject[] results = new JavaScriptObject[asyncs.size()];
                for (int i = 0; i < asyncs.size(); i++) {
                    JavaScriptObject object = GwtClientUtils.newObject();
                    GAsync suggestion = asyncs.get(i);
                    GwtClientUtils.setField(object, "displayString", fromString(PValue.getStringValue(suggestion.getDisplayValue())));
                    GwtClientUtils.setField(object, "rawString", fromString(PValue.getStringValue(suggestion.getRawValue())));
                    GwtClientUtils.setField(object, "key", fromObject(suggestion.key));
                    results[i] = object;
                }
                JavaScriptObject data = GwtClientUtils.newObject();
                GwtClientUtils.setField(data, "data", fromObject(results));
                GwtClientUtils.setField(data, "more", fromBoolean(result.moreRequests));
                GwtClientUtils.call(successCallBack, data);
         }});
    }

    private NativeHashMap<GGroupObjectValue, JavaScriptObject> oldOptionsList = new NativeHashMap<>();
    private JavaScriptObject getDiff(JsArray<JavaScriptObject> list, boolean supportReordering) {
        return getDiff(list, supportReordering, new DiffObjectInterface<GGroupObjectValue, JavaScriptObject>() {
            @Override
            public GGroupObjectValue getKey(JavaScriptObject object) {
                return GSimpleStateTableView.this.getKey(object);
            }

            @Override
            public NativeHashMap<GGroupObjectValue, JavaScriptObject> getOldObjectsList() {
                return GSimpleStateTableView.this.oldOptionsList;
            }

            @Override
            public void setOldObjectsList(NativeHashMap<GGroupObjectValue, JavaScriptObject> optionsList) {
                GSimpleStateTableView.this.oldOptionsList = optionsList;
            }
        });
    }

    public static <K, V extends JavaScriptObject> JavaScriptObject getDiff(JsArray<V> list, boolean supportReordering, DiffObjectInterface<K, V> diffObjectInterface) {
        NativeHashMap<K, V> oldOptionsList = diffObjectInterface.getOldObjectsList();
        List<JavaScriptObject> optionsToAdd = new ArrayList<>();
        List<JavaScriptObject> optionsToUpdate = new ArrayList<>();
        List<JavaScriptObject> optionsToRemove = new ArrayList<>();

        NativeHashMap<K, V> mappedList = new NativeHashMap<>();
        for (int i = 0; i < list.length(); i++) {
            V object = list.get(i);
            GwtClientUtils.setField(object, "index", fromDouble(i));
            K key = diffObjectInterface.getKey(object);
            mappedList.put(key, object);
            JavaScriptObject oldValue = oldOptionsList.remove(key);
            if (oldValue != null) {
                if (!GwtClientUtils.isJSObjectPropertiesEquals(object, oldValue)) {
                    if (supportReordering && Integer.parseInt(GwtClientUtils.getField(oldValue, "index").toString()) != i) {
                        optionsToRemove.add(oldValue);
                        optionsToAdd.add(object);
                    } else {
                        optionsToUpdate.add(object);
                    }
                }
            } else {
                optionsToAdd.add(object);
            }
        }

        oldOptionsList.foreachValue(optionsToRemove::add);
        diffObjectInterface.setOldObjectsList(mappedList);

        JavaScriptObject object = GwtClientUtils.newObject();
        GwtClientUtils.setField(object, "add", fromObject(optionsToAdd.toArray()));
        GwtClientUtils.setField(object, "update", fromObject(optionsToUpdate.toArray()));
        GwtClientUtils.setField(object, "remove", GwtClientUtils.sortArray(fromObject(optionsToRemove.toArray()), "index", true));
        return object;
    }

    protected native JavaScriptObject getController()/*-{
        var thisObj = this;
        return {
            changeProperty: function (property, object, newValue) {
                if(object === undefined)
                    object = null;
                if(newValue === undefined) // not passed
                    newValue = @GSimpleStateTableView::UNDEFINED;
                return thisObj.@GSimpleStateTableView::changeJSProperty(*)(property, object, newValue);
            },
            changeDateTimeProperty: function (property, object, year, month, day, hour, minute, second) {
                return thisObj.@GSimpleStateTableView::changeDateTimeProperty(*)(property, object, year, month, day, hour, minute, second);
            },
            changeDateProperty: function (property, object, year, month, day) {
                return thisObj.@GSimpleStateTableView::changeDateProperty(*)(property, object, year, month, day);
            },
            changeProperties: function (properties, objects, newValues) {
                return thisObj.@GSimpleStateTableView::changeJSProperties(*)(properties, objects, newValues);
            },
            changeDateTimeProperties: function (properties, objects, years, months, days, hours, minutes, seconds) {
                return thisObj.@GSimpleStateTableView::changeDateTimeProperties(*)(properties, objects, years, months, days, hours, minutes, seconds);
            },
            changeDateProperties: function (properties, objects, years, months, days) {
                return thisObj.@GSimpleStateTableView::changeDateProperties(*)(properties, objects, years, months, days);
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
            getValueClass: function (property, object) {
                return thisObj.@GSimpleStateTableView::getValueElementClass(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getBackground: function (property, object) {
                return thisObj.@GSimpleStateTableView::getBackground(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            getForeground: function (property, object) {
                return thisObj.@GSimpleStateTableView::getForeground(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            changeObject: function (object, rendered, elementClicked) {
                if(rendered === undefined)
                    rendered = false;
                if(elementClicked === undefined)
                    elementClicked = null;
                return thisObj.@GSimpleStateTableView::changeSimpleGroupObject(*)(object, rendered, elementClicked);
            },
            setDateIntervalViewFilter: function (property, pageSize, startYear, startMonth, startDay, endYear, endMonth, endDay, isDateTimeFilter) {
                thisObj.@GSimpleStateTableView::setDateIntervalViewFilter(*)(property, pageSize, startYear, startMonth, startDay, endYear, endMonth, endDay, isDateTimeFilter);
            },
            setBooleanViewFilter: function (property, pageSize) {
                thisObj.@GSimpleStateTableView::setBooleanViewFilter(*)(property, pageSize);
            },
            getCurrentDay: function (propertyName) {
                return thisObj.@GSimpleStateTableView::getPropertyJsValue(*)(propertyName);
            },
            getGroupObjectBackgroundColor: function(object) {
                var color = object.color;
                if (color)
                    return color.toString();
                return thisObj.@GStateTableView::getRowBackgroundColor(*)(thisObj.@GSimpleStateTableView::getKey(*)(object));
            },
            getDisplayBackgroundColor: function (color, isCurrentKey) {
                return @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(color);
            },
            getDisplayForegroundColor: function (color) {
                return @lsfusion.gwt.client.base.view.ColorUtils::getThemedColor(Ljava/lang/String;)(color);
            },
            getGroupObjectForegroundColor: function(object) {
                return thisObj.@GStateTableView::getRowForegroundColor(*)(thisObj.@GSimpleStateTableView::getKey(*)(object));
            },
            getValues: function (property, value, successCallback, failureCallback) {
                return thisObj.@GSimpleStateTableView::getAsyncValues(*)(property, value, successCallback, failureCallback);
            },
            getKey: function (object) {
                return thisObj.@GSimpleStateTableView::getKey(*)(object);
            },
            getDiff: function (newList, supportReordering) {
                return thisObj.@GSimpleStateTableView::getDiff(Lcom/google/gwt/core/client/JsArray;Z)(newList, supportReordering);
            },
            getColorThemeName: function () {
                return @lsfusion.gwt.client.view.MainFrame::colorTheme.@java.lang.Enum::name()();
            },
            isList: function () {
                return true;
            }
        };
    }-*/;
}
