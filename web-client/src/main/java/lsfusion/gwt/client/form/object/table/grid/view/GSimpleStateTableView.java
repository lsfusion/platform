package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.view.Column;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;

public abstract class GSimpleStateTableView<P> extends GStateTableView {

    protected final JavaScriptObject controller;

    public GSimpleStateTableView(GFormController form, GGridController grid) {
        super(form, grid);

        this.controller = getController();
        getDrawElement().getStyle().setProperty("zIndex", "0"); // need this because views like leaflet and some others uses z-indexes and therefore dialogs for example are shown below layers
    }

    private NativeHashMap<String, Column> columnMap;

    @Override
    protected void updateView() {
        columnMap = new NativeHashMap<>();
        JsArray<JavaScriptObject> list = convertToObjectsMixed(getData(columnMap));

        if(popupObject != null && !isCurrentKey(popupKey)) // if another current key set hiding popup
            hidePopup();

        render(getDrawElement(), list);
    }

    protected abstract void render(Element element, JsArray<JavaScriptObject> list);

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
            NativeHashMap<GGroupObjectValue, Object> propValues = values.get(i);

            for (int j = 0; j < propColumnKeys.size(); j++) {
                GGroupObjectValue columnKey = propColumnKeys.get(j);
                if(checkShowIf(i, columnKey))
                    continue;

                GGroupObjectValue fullKey = key != null ? GGroupObjectValue.getFullKey(key, columnKey) : GGroupObjectValue.EMPTY;

                pushValue(rowValues, property, propValues.get(fullKey));
            }
        }
        rowValues.push(fromObject(key));
        return rowValues;
    }

    private void pushValue(JsArray<JavaScriptObject> array, GPropertyDraw property, Object value) {
        if(property.baseType instanceof GLogicalType)
            array.push(fromBoolean(value!=null));
        else if(value == null)
            array.push(null);
        else if(property.baseType instanceof GIntegralType)
            array.push(fromNumber(((Number)value).doubleValue()));
        else if(property.baseType instanceof GImageType)
            array.push(fromString(GwtClientUtils.getDownloadURL((String) value, null, ((GImageType)property.baseType).extension, false)));
        else
            array.push(fromString(value.toString()));
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

                String columnName = property.integrationSID + (columnKey.isEmpty() ? "" : "_" + c);
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
        GGroupObjectValue key = toObject(object);
        long requestIndex = changeGroupObject(key, rendered);

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
        popupObject = showPopup(popupElementClicked, grid.recordView.getElement());

        popupRequestIndex = -2; // we are no longer waiting for popup
        popupElementClicked = null; // in theory it's better to do it on popupObject close, but this way is also ok
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

    protected void changeProperty(String property, Serializable newValue, JavaScriptObject object) {
        Column column = columnMap.get(property);
        changeProperty(column.property, toObject(object), column.columnKey, newValue);
    }

    protected boolean isReadOnly(String property, GGroupObjectValue object) {
        Column column = columnMap.get(property);
        return isReadOnly(column.property, object, column.columnKey);
    }

    protected boolean isReadOnly(String property, JavaScriptObject object) {
        return isReadOnly(property, getKey(object));
    }

    protected boolean isCurrentObjectKey(JavaScriptObject object){
        return isCurrentKey(getKey(object));
    }

    protected GGroupObjectValue getKey(JavaScriptObject object) {
        return toObject(getValue(object, keysFieldName));
    }
    protected NativeHashMap<GGroupObjectValue, JavaScriptObject> buildKeyMap(JsArray<JavaScriptObject> objects) {
        NativeHashMap<GGroupObjectValue, JavaScriptObject> map = new NativeHashMap<>();
        for(int i=0,size=objects.length();i<size;i++) {
            JavaScriptObject object = objects.get(i);
            map.put(toObject(getValue(object, keysFieldName)), object);
        }
        return map;        
    }

    protected void changeObjectProperty(String property, JavaScriptObject object, Serializable newValue) {
        changeProperty(property, newValue, fromObject(getKey(object)));
    }

    protected void changeDateTimeProperty(String property, JavaScriptObject object, int year, int month, int day, int hour, int minute, int second) {
        changeObjectProperty(property, object, new GDateTimeDTO(year, month, day, hour, minute, second));
    }

    protected void changeDateProperty(String property, JavaScriptObject object, int year, int month, int day) {
        changeObjectProperty(property, object, new GDateDTO(year, month, day));
    }

    protected native JavaScriptObject getController()/*-{
        var thisObj = this;
        return {
            changeProperty: function (property, object, newValue) {
                return thisObj.@GSimpleStateTableView::changeObjectProperty(*)(property, object, newValue);
            },
            changeDateTimeProperty: function (property, object, year, month, day, hour, minute, second) {
                return thisObj.@GSimpleStateTableView::changeDateTimeProperty(*)(property, object, year, month, day, hour, minute, second);
            },
            changeDateProperty: function (property, object, year, month, day) {
                return thisObj.@GSimpleStateTableView::changeDateProperty(*)(property, object, year, month, day);
            },
            isCurrent: function (object) {
                return thisObj.@GSimpleStateTableView::isCurrentObjectKey(*)(object);
            },
            isPropertyReadOnly: function (property, object) {
                return thisObj.@GSimpleStateTableView::isReadOnly(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(property, object);
            },
            changeSimpleGroupObject: function (object, rendered, elementClicked) {
                var jsObject = thisObj.@GSimpleStateTableView::fromObject(*)(thisObj.@GSimpleStateTableView::getKey(*)(object));
                return thisObj.@GSimpleStateTableView::changeSimpleGroupObject(*)(jsObject, rendered, elementClicked);
            }
        };
    }-*/;
}
