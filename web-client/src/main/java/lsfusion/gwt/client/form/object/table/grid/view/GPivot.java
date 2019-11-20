package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyGroupType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GPivot extends GStateTableView {
    
    public GPivot(GFormController formController, GGridController gridController) {
        super(formController, gridController);

        setStyleName(getElement(), "pivotTable");

        config = getDefaultConfig(COLUMN, VALUE);
        rerender = true;
    }

    private static class Column {
        public final GPropertyDraw property;
        public final GGroupObjectValue columnKey;

        public Column(GPropertyDraw property, GGroupObjectValue columnKey) {
            this.property = property;
            this.columnKey = columnKey;
        }
    }

    private JsArray<JavaScriptObject> convertToObjects(JsArray<JsArrayString> array) {
        JsArrayString columns = array.get(0);
        JsArray<JavaScriptObject> convert = JavaScriptObject.createArray().cast();
        for(int i=1;i<array.length();i++) {
            WrapperObject object = JavaScriptObject.createObject().cast();
            JsArrayString values = array.get(i);
            for(int j=0;j<columns.length();j++) {
                object.putValue(columns.get(j), values.get(j));
            }
            convert.push(object);
        }
        return convert;
    }

    // depending on view, we may create regular view or key / value view
    private JsArray<JsArrayString> getArray(Map<String, Column> columnMap, List<String> aggrCaptions) {
        JsArray<JsArrayString> array = JavaScriptObject.createArray().cast();

        boolean first = true;
        JsArrayString rowCaptions = JavaScriptObject.createArray().cast();
        for (GGroupObjectValue key : keys != null && !keys.isEmpty() ? keys : Collections.singleton((GGroupObjectValue)null)) { // can be null if manual update
            List<Consumer<JsArrayString>> aggrValues = new ArrayList<>();

            JsArrayString rowValues = JavaScriptObject.createArray().cast();
            for (int i = 0; i < properties.size(); i++) {
                GPropertyDraw property = properties.get(i);
                Map<GGroupObjectValue, Object> propCaptions = captions.get(i);
                Map<GGroupObjectValue, Object> propValues = values.get(i);
                boolean isAggr = property.baseType instanceof GIntegralType;

                for (GGroupObjectValue columnKey : columnKeys.get(i)) {
                    String caption = null;
                    if (first || isAggr) {
                        if (propCaptions != null)
                            caption = property.getDynamicCaption(propCaptions.get(columnKey));
                        else
                            caption = property.getCaptionOrEmpty();
                    }

                    if (first) {
                        columnMap.put(caption, new Column(property, columnKey));
                        rowCaptions.push(caption);
                    }

                    Consumer<JsArrayString> pushArray;
                    Object value = key != null ? propValues.get(GGroupObjectValue.getFullKey(key, columnKey)) : null; // we need row of nulls (otherwise pivot table doesn't show anything)
                    if (isAggr) {
                        pushArray = rv -> rv.push(value != null ? Double.toString(((Number) value).doubleValue()) : null);

                        if(first)
                            aggrCaptions.add(caption);
                        aggrValues.add(pushArray);
                    } else {
                        if (property.baseType instanceof GLogicalType) {
                            pushArray = rv -> rv.push(value != null ? "true" : "false");
                        } else
                            pushArray = rv -> rv.push(value != null ? value.toString() : null);
                    }
                    pushArray.accept(rowValues);
                }
            }

            if(first) {
                rowCaptions.push(COLUMN);
                rowCaptions.push(VALUE);
                array.push(rowCaptions);

                first = false;
            }

            for(int i=0,size=aggrCaptions.size();i<size;i++) {
                JsArrayString aggrRowValues = clone(rowValues);
                aggrRowValues.push(aggrCaptions.get(i));
                aggrValues.get(i).accept(aggrRowValues);

                array.push(aggrRowValues);
            }
        }
        return array;
    }

    private static final String COLUMN = "(Колонка)";
    private static final String VALUE = "(Значение)";

    @Override
    protected void updateView(boolean dataUpdated, Boolean updateState) {
        if(updateState != null)
            this.updateState = updateState;

        com.google.gwt.dom.client.Element element = getElement();
        if(dataUpdated || rerender) {
            columnMap = new NativeHashMap<>();
            aggrCaptions = new ArrayList<>();
            JavaScriptObject data = convertToObjects(getArray(columnMap, aggrCaptions));

            render(element, data, config, rerender); // we need to updateRendererState after it is painted
            rerender = false;
        }

        updateRendererState(this.updateState); // update state with server response
    }

    private Map<String, Column> columnMap;
    private List<String> aggrCaptions;

    boolean updateState;
    WrapperObject config;
    boolean rerender = false;

    private boolean settings = true;
    public boolean isSettings() {
        return settings;
    }
    public void switchSettings() {
        settings = !settings;
        config = overrideShowUI(config, settings);
        rerender = true;
        updateView(false, null);
    }

    private static class WrapperObject extends JavaScriptObject {
        protected WrapperObject() {
        }

        protected native final JsArrayString getKeys() /*-{
            return Object.keys(this);
        }-*/;
        protected native final JsArrayString getArrayString(String string) /*-{
            return this[string];
        }-*/;
        protected native final void putValue(String key, Object object) /*-{
            this[key] = object;
        }-*/;
        protected native final Object getValue(String key) /*-{
            return this[key];
        }-*/;
    }

    private void fillGroupColumns(JsArrayString cols, List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeys, List<GPropertyGroupType> types, List<String> aggrColumns) {
        for(int i=0,size=cols.length();i<size;i++) {
            String name = cols.get(i);
            if(!name.equals(COLUMN)) {
                Column col = columnMap.get(name);
                properties.add(col.property);
                columnKeys.add(col.columnKey);
                types.add(GPropertyGroupType.GROUP);

                removeAggrFilters(name, aggrColumns);
            }
        }
    }

    private void removeAggrFilters(String name, List<String> aggrColumns) {
        if(aggrColumns.remove(name)) { // if there was aggr column in filters -> remove it from filters
            applyFilter(COLUMN, aggrColumns, this.aggrCaptions);
        }
    }

    private void applyFilter(String name, List<String> include, List<String> allValues) {
        JsArrayString inclusions = JavaScriptObject.createArray().cast();
        JsArrayString exclusions = JavaScriptObject.createArray().cast();
        for(String aggrCaption : allValues)
            (include.contains(aggrCaption) ? inclusions : exclusions).push(aggrCaption);
        config = overrideFilter(config, name, inclusions, exclusions);
        rerender = true;
    }

    private native WrapperObject overrideFilter(WrapperObject config, String column, JsArrayString columnInclusions, JsArrayString columnExclusions)/*-{
        var newInclusions = {};
        newInclusions[column] = columnInclusions;
        var newExclusions = {};
        newExclusions[column] = columnExclusions;
        return Object.assign({}, config, {
            inclusions : Object.assign({}, config.inclusions, newInclusions),
            exclusions : Object.assign({}, config.exclusions, newExclusions)
        });
    }-*/;

    private native WrapperObject overrideDataClass(WrapperObject config, boolean subTotal)/*-{
        return Object.assign({}, config, {
            dataClass : (subTotal ? $wnd.$.pivotUtilities.SubtotalPivotData : $wnd.$.pivotUtilities.PivotData)
        });
    }-*/;

    private native WrapperObject overrideShowUI(WrapperObject config, boolean showUI)/*-{
        return Object.assign({}, config, {
            showUI : showUI
        });
    }-*/;

    private List<String> createAggrColumns(WrapperObject inclusions) {
        JsArrayString columnValues = inclusions.getArrayString(COLUMN);
        if(columnValues == null)
            return new ArrayList<>(aggrCaptions); // all columns

        List<String> result = new ArrayList<>();
        for(int i=0,size=columnValues.length();i<size;i++)
            result.add(columnValues.get(i));
        return result;
    }

//    private boolean isSubTotal(String rendererName) {
//        return rendererName != null && rendererName.contains("Subtotal");
//    }

    private void onRefresh(WrapperObject config, JsArrayString rows, JsArrayString cols, WrapperObject inclusions, String aggregatorName, String rendererName) {
        // to get rid of blinking always use SubtotalPivotData
//        boolean wasSubTotal = isSubTotal((String) this.config.getValue("rendererName"));

        this.config = config;

//        boolean isSubTotal = isSubTotal(rendererName);
//        if(isSubTotal != wasSubTotal) {
//            this.config = overrideDataClass(this.config, isSubTotal);
//            overwriteConfig = true;
//        }

        List<GPropertyDraw> properties = new ArrayList<>();
        List<GGroupObjectValue> columnKeys = new ArrayList<>();
        List<GPropertyGroupType> types = new ArrayList<>();

        List<String> aggrColumns = createAggrColumns(inclusions);

        fillGroupColumns(rows, properties, columnKeys, types, aggrColumns);
        fillGroupColumns(cols, properties, columnKeys, types, aggrColumns);

        for(String aggrColumnCaption : aggrColumns) {
            Column aggrColumn = columnMap.get(aggrColumnCaption);
            properties.add(aggrColumn.property);
            columnKeys.add(aggrColumn.columnKey);
            types.add(GPropertyGroupType.valueOf(aggregatorName.toUpperCase()));
        }

        updateRendererState(true); // will wait until server will answer us if we need to change something
        grid.changeGroupMode(properties, columnKeys, types);
    }

    private Element rendererElement; // we need to save renderer element, since it is asynchronously replaced, and we might update old element (that is just about to disappear)
    private void setRendererElement(Element element) {
        rendererElement = element;
    }
    private void updateRendererState(boolean set) {
        updateRendererElementState(rendererElement, set);
    }
    private native void updateRendererElementState(com.google.gwt.dom.client.Element element, boolean set) /*-{
        return $wnd.$(element).find(".pvtRendererArea").css('filter', set ? 'opacity(0.5)' : 'opacity(1)');
    }-*/;

    private native WrapperObject getDefaultConfig(String columnField, String valueField)/*-{
        var tpl = $wnd.$.pivotUtilities.aggregatorTemplates;
        var instance = this;

        var renderers = $wnd.$.extend(
            $wnd.$.pivotUtilities.subtotal_renderers,
            $wnd.$.pivotUtilities.plotly_renderers,
//            $wnd.$.pivotUtilities.c3_renderers,
            $wnd.$.pivotUtilities.renderers,
            $wnd.$.pivotUtilities.d3_renderers
        );

        return {
            dataClass : $wnd.$.pivotUtilities.SubtotalPivotData,
            cols : [columnField], // inital columns since overwrite is false
            hiddenFromDragDrop : [valueField],
            renderers : renderers,
            aggregators: {
                "Sum": function() { return tpl.sum()([valueField]) },
//                "Count": function() { return tpl.sum()([valueField]) }, // doesn't work now because should be different in server and client mode
                "Max": function() { return tpl.max()([valueField]) },
                "Min": function() { return tpl.min()([valueField])}
            },
            onRefresh: function(config) {
                instance.@GPivot::onRefresh(*)(config, config.rows, config.cols, config.inclusions, config.aggregatorName, config.rendererName);
            }
        }
    }-*/;

    protected native void render(com.google.gwt.dom.client.Element element, JavaScriptObject array, JavaScriptObject config, boolean overwrite)/*-{
//        var d = element;
        var d = $doc.createElement('div');

        if (true) // because we create new element every time
            $wnd.$(d).pivotUI(array, config, true);
        else
            $wnd.$(d).pivotUI(array);

        this.@GPivot::setRendererElement(*)(d);

        // it's tricky in pivotUI, first refresh is with timeout 10ms, and that's why there is a blink, when pivotUI is painted with empty Renderer
        // to fix this will add it to the visible DOM, in 15 ms (after everything is painted)
        setChild = function () {
            if (element.hasChildNodes())
                element.removeChild(element.childNodes[0]);
            element.appendChild(d);
        };
        setTimeout(setChild, 15);
    }-*/;
}
