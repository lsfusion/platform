package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.view.Column;

import java.io.Serializable;
import java.util.*;

public abstract class GSimpleStateTableView extends GStateTableView {

    public GSimpleStateTableView(GFormController form, GGridController grid) {
        super(form, grid);
        getDrawElement().getStyle().setProperty("zIndex", "0"); // need this because views like leaflet and some others uses z-indexes and therefore dialogs for example are shown below layers
    }

    private NativeHashMap<String, Column> columnMap;

    @Override
    protected void updateView() {
        columnMap = new NativeHashMap<>();
        JsArray<JavaScriptObject> list = convertToObjectsMixed(getData(columnMap));

        render(getDrawElement(), getRecordElement(), list);
    }

    protected abstract void render(Element element, com.google.gwt.dom.client.Element recordElement, JsArray<JavaScriptObject> list);

    @Override
    protected Element getRendererAreaElement() {
        return getElement();
    }

    // we need key / value view since pivot
    private JsArray<JsArray<JavaScriptObject>> getData(NativeHashMap<String, Column> columnMap) {
        JsArray<JsArray<JavaScriptObject>> array = JavaScriptObject.createArray().cast();

        array.push(getCaptions(columnMap));

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

    private JsArray<JavaScriptObject> getCaptions(NativeHashMap<String, Column> columnMap) {
        JsArray<JavaScriptObject> columns = JavaScriptObject.createArray().cast();
        for (int i = 0, size = properties.size() ; i < size; i++) {
            GPropertyDraw property = properties.get(i);
            List<GGroupObjectValue> propColumnKeys = columnKeys.get(i);

            for (int c = 0; c < propColumnKeys.size(); c++) {
                GGroupObjectValue columnKey = propColumnKeys.get(c);
                String columnName = property.integrationSID + (columnKey.isEmpty() ? "" : "_" + c);
                columnMap.put(columnName, new Column(property, columnKey));
                columns.push(fromString(columnName));
            }
        }
        columns.push(fromString(keysFieldName));
        return columns;
    }
    
    protected final static String keysFieldName = "#__key";

    protected void changeSimpleGroupObject(JavaScriptObject object) {
        changeGroupObject(toObject(object));
    }

    protected void changeProperty(String property, Serializable newValue, JavaScriptObject object) {
        Column column = columnMap.get(property);
        changeProperty(column.property, toObject(object), column.columnKey, newValue);
    }

    protected boolean isReadOnly(String property, GGroupObjectValue object) {
        Column column = columnMap.get(property);
        return isReadOnly(column.property, object, column.columnKey);
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

    protected native void getController()/*-{
        var thisObj = this;
        return {
            changeProperty: function (property, object, newValue) {
                return thisObj.@GSimpleStateTableView::changeObjectProperty(*)(property, object, newValue);
            },
            changeDateTimeProperty: function (property, object, year, month, day, hour, minute, second) {
                return thisObj.@GSimpleStateTableView::changeDateTimeProperty(*)(property, object, year, month, day, hour, minute, second);
            }
        };
    }-*/;
}
