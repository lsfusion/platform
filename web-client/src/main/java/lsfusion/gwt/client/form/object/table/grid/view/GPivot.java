package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyGroupType;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.*;

public class GPivot extends GStateTableView {
    public GPivot(GFormController formController, GGridController gridController) {
        super(formController, gridController);

        setStyleName(getElement(), "pivotTable");

        config = getDefaultConfig(COLUMN);
    }

    // in theory we can order all properties once, but so far there is no full list of properties
    private void fillPropertiesOrder(List<GPropertyDraw> properties, List<GPropertyDraw> propertiesList, Set<GPropertyDraw> propertiesSet) {
        for (GPropertyDraw property : properties) {
            if (propertiesSet.add(property)) {
                if (property.formula != null) {
                    fillPropertiesOrder(property.formulaOperands, propertiesList, propertiesSet);
                }
                propertiesList.add(property);
            }
        }
    }

    // we need key / value view since pivot
    private JsArray<JsArrayMixed> getData(Map<String, Column> columnMap, Aggregator aggregator, List<String> aggrCaptions, JsArrayString systemCaptions, boolean convertDataToStrings) {
        JsArray<JsArrayMixed> array = JavaScriptObject.createArray().cast();

        array.push(getCaptions(columnMap, aggregator, aggrCaptions, systemCaptions));

        // getting values
        for (GGroupObjectValue key : keys != null && !keys.isEmpty() ? keys : Collections.singleton((GGroupObjectValue) null)) { // can be null if manual update
            JsArrayMixed rowValues = getValues(key, convertDataToStrings);

            for (String aggrCaption : aggrCaptions) { // putting columns to rows
                JsArrayMixed aggrRowValues = clone(rowValues);
                aggrRowValues.push(aggrCaption);
                array.push(aggrRowValues);
            }
        }
        return array;
    }

    private JsArrayMixed getValues(GGroupObjectValue key, boolean convertDataToStrings) {
        JsArrayMixed rowValues = JavaScriptObject.createArray().cast();
        for (int i = 0; i < properties.size(); i++) {
            List<GGroupObjectValue> propColumnKeys = columnKeys.get(i);
            Map<GGroupObjectValue, Object> propValues = values.get(i);
            List<Map<GGroupObjectValue, Object>> propLastAggrs = lastAggrs.get(i);

            GridCellRenderer renderer = null;
            if(convertDataToStrings)
                renderer = properties.get(i).getGridCellRenderer();

            for (GGroupObjectValue columnKey : propColumnKeys) {
                GGroupObjectValue fullKey = key != null ? GGroupObjectValue.getFullKey(key, columnKey) : GGroupObjectValue.EMPTY;

                pushValue(rowValues, propValues, fullKey, renderer);
                for (Map<GGroupObjectValue, Object> propLastAggr : propLastAggrs) {
                    pushValue(rowValues, propLastAggr, fullKey, renderer);
                }
            }
        }
        return rowValues;
    }

    private JsArrayMixed getCaptions(Map<String, Column> columnMap, Aggregator aggregator, List<String> aggrCaptions, JsArrayString systemCaptions) {
        // we need correct formulas order
        List<GPropertyDraw> orderedProperties = new ArrayList<>();
        fillPropertiesOrder(properties, orderedProperties, new HashSet<>());
        Map<GPropertyDraw, Map<GGroupObjectValue, ColumnAggregator>> aggregators = new HashMap<>();
        Map<GPropertyDraw, List<String>> orderedColumns = new HashMap<>();

        for (GPropertyDraw property : orderedProperties) {
            ArrayList<String> propColumns = new ArrayList<>();
            orderedColumns.put(property, propColumns);

            int baseOrder = properties.indexOf(property);
            List<GGroupObjectValue> propColumnKeys = columnKeys.get(baseOrder);
            Map<GGroupObjectValue, Object> propCaptions = captions.get(baseOrder);
            List<Map<GGroupObjectValue, Object>> propLastAggrs = lastAggrs.get(baseOrder);

            for (GGroupObjectValue columnKey : propColumnKeys) {
                String caption = getPropertyCaption(propCaptions, property, columnKey);

                columnMap.put(caption, new Column(property, columnKey));

                propColumns.add(caption);

                JsArrayString lastColumns = JavaScriptObject.createArray().cast();
                for (int j = 0, size = propLastAggrs.size(); j < size; j++) {
                    String lastCaption = caption + "$_" + j;
                    lastColumns.push(lastCaption);

                    propColumns.add(lastCaption);
                    systemCaptions.push(lastCaption);
                }

                ColumnAggregator columnAggregator = getGroupAggregator(property, lastColumns);

                if (property.formula != null) {
                    columnAggregator.setID(caption);
                    columnAggregator = getFormulaAggregator(property, columnKey, columnAggregator, aggregators);
                }

                columnAggregator.setID(caption);
                aggregators.computeIfAbsent(property, p -> new HashMap<>()).put(columnKey, columnAggregator);

                aggregator.setAggregator(caption, columnAggregator);

                if (property.baseType instanceof GIntegralType)
                    aggrCaptions.add(caption);
            }
        }

        // ordering in inital order (all other lists are actually sets)
        JsArrayMixed rowCaptions = JavaScriptObject.createArray().cast();
        for (int i = 0; i < properties.size(); i++)
            for (String column : orderedColumns.get(properties.get(i)))
                rowCaptions.push(column);
        rowCaptions.push(COLUMN); // putting columns to rows

        return rowCaptions;
    }

    private void pushValue(JsArrayMixed rowValues, Map<GGroupObjectValue, Object> propValues, GGroupObjectValue fullKey, GridCellRenderer cellRenderer) {
        Object value = propValues.get(fullKey);
        rowValues.push(value != null ? fromObject(cellRenderer != null ? cellRenderer.format(value) : value) : null);
    }

    private String getPropertyCaption(Map<GGroupObjectValue, Object> propCaptions, GPropertyDraw property, GGroupObjectValue columnKey) {
        String caption;
        if (propCaptions != null)
            caption = property.getDynamicCaption(propCaptions.get(columnKey));
        else
            caption = property.getCaptionOrEmpty();
        return caption;
    }

    public static final String COLUMN = "(Колонка)";

    @Override
    protected void updateView() {
        columnMap = new NativeHashMap<>();
        aggrCaptions = new ArrayList<>();
        Aggregator aggregator = Aggregator.create();
        JsArrayString systemColumns = JavaScriptObject.createArray().cast();
        boolean convertDataToStrings = false; // so far we'll not use renderer formatters and we'll rely on native toString (if we decide to do it we'll have to track renderer type and rerender everything if this type changes that can may lead to some blinking)
        JavaScriptObject data = getData(columnMap, aggregator, aggrCaptions, systemColumns, convertDataToStrings); // convertToObjects()
        config = overrideAggregators(config, getAggregators(aggregator), systemColumns);
        config = overrideCallbacks(config, getCallbacks());
        
        int rowHeight = 0;
        for(GPropertyDraw property : properties) {
            int columnMinimumHeight = property.getValueHeight(font);
            rowHeight = Math.max(rowHeight, columnMinimumHeight);
        }
        this.rowHeight = rowHeight;

        JsArrayString jsArray = JsArrayString.createArray().cast();
        aggrCaptions.forEach(jsArray::push);

        render(getElement(), data, config, jsArray); // we need to updateRendererState after it is painted
    }

    @Override
    public void runGroupReport(boolean toExcel) {
        if (toExcel) {
            exportToExcel(getElement());
        } else {
            exportToPdf(getElement());
        }
    }

    public static native void exportToExcel(Element element)
        /*-{
            var pvtTable = element.getElementsByClassName("pvtTable")[0];

            //set bold
            Array.from(pvtTable.querySelectorAll("th.pvtAxisLabel, th.pvtColLabel, th.pvtTotalLabel, th.pvtTotalLabel, th.pvtRowLabel, td.pvtTotal, td.pvtRowSubtotal, td.pvtGrandTotal")).forEach(function (item) {
                item.setAttribute("data-f-bold", "true");
            });

            var workbook = $wnd.TableToExcel.tableToBook(pvtTable, {
                sheet: {
                    name: "lsfReport"
                }
            });

            //set column width
            var worksheet = workbook.getWorksheet(1);
            for (var i = 0; i < worksheet.columns.length; i += 1) {
                var dataMax = 0;
                var column = worksheet.columns[i];
                for (var j = 1; j < column.values.length; j += 1) {
                    var columnLength = column.values[j].length;
                    if (columnLength > dataMax) {
                        dataMax = columnLength;
                    }
                }
                column.width = (dataMax < 10 ? 10 : dataMax) * 1.2; //1.2 is magic coefficient to better fit width
            }

            $wnd.TableToExcel.save(workbook, "lsfReport.xlsx");
        }-*/;

    public static native void exportToPdf(Element element)
        /*-{
            var docDefinition = {
                pageOrientation: 'landscape',
                content: [
                    $wnd.htmlToPdfmake(element.getElementsByClassName("pvtTable")[0].outerHTML)
                ]
            };
            $wnd.pdfMake.createPdf(docDefinition).download('lsfReport.pdf');
        }-*/;

    private Map<String, Column> columnMap;
    private List<String> aggrCaptions;
    private WrapperObject config;
    private boolean settings = true;

    private int rowHeight;

    public boolean isSettings() {
        return settings;
    }

    public void switchSettings() {
        settings = !settings;
        config = overrideShowUI(config, settings);
        rerender();

        updateView(false, null);
    }

    private void fillGroupColumns(JsArrayString cols, List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeys, List<GPropertyGroupType> types, List<String> aggrColumns) {
        for (int i = 0, size = cols.length(); i < size; i++) {
            String name = cols.get(i);
            if (!name.equals(COLUMN)) {
                Column col = columnMap.get(name);
                properties.add(col.property);
                columnKeys.add(col.columnKey);
                types.add(GPropertyGroupType.GROUP);

                removeAggrFilters(name, aggrColumns);
            }
        }
    }

    private void removeAggrFilters(String name, List<String> aggrColumns) {
        if (aggrColumns.remove(name)) { // if there was aggr column in filters -> remove it from filters
            applyFilter(COLUMN, aggrColumns, this.aggrCaptions);
        }
    }

    private void applyFilter(String name, List<String> include, List<String> allValues) {
        JsArrayString inclusions = JavaScriptObject.createArray().cast();
        JsArrayString exclusions = JavaScriptObject.createArray().cast();
        for (String aggrCaption : allValues)
            (include.contains(aggrCaption) ? inclusions : exclusions).push(aggrCaption);
        config = overrideFilter(config, name, inclusions, exclusions);
        rerender();
    }

    private native WrapperObject overrideFilter(WrapperObject config, String column, JsArrayString columnInclusions, JsArrayString columnExclusions)/*-{
        var newInclusions = {};
        newInclusions[column] = columnInclusions;
        var newExclusions = {};
        newExclusions[column] = columnExclusions;
        return Object.assign({}, config, {
            inclusions: Object.assign({}, config.inclusions, newInclusions),
            exclusions: Object.assign({}, config.exclusions, newExclusions)
        });
    }-*/;

    private native WrapperObject overrideDataClass(WrapperObject config, boolean subTotal)/*-{
        return Object.assign({}, config, {
            dataClass: (subTotal ? $wnd.$.pivotUtilities.SubtotalPivotData : $wnd.$.pivotUtilities.PivotData)
        });
    }-*/;

    private native WrapperObject overrideShowUI(WrapperObject config, boolean showUI)/*-{
        return Object.assign({}, config, {
            showUI: showUI
        });
    }-*/;

    private native WrapperObject overrideAggregators(WrapperObject config, JavaScriptObject aggregators, JsArrayString systemColumns)/*-{
        return Object.assign({}, config, {
            aggregators: aggregators,
            hiddenFromDragDrop: systemColumns
        });
    }-*/;

    private native WrapperObject overrideCallbacks(WrapperObject config, JavaScriptObject callbacks)/*-{
        return Object.assign({}, config, {
            callbacks: callbacks
        });
    }-*/;
    
    private List<String> createAggrColumns(WrapperObject inclusions) {
        JsArrayString columnValues = inclusions.getArrayString(COLUMN);
        if (columnValues == null)
            return new ArrayList<>(aggrCaptions); // all columns

        List<String> result = new ArrayList<>();
        for (int i = 0, size = columnValues.length(); i < size; i++)
            result.add(columnValues.get(i));
        return result;
    }

//    private boolean isTable = true;

    private void onRefresh(WrapperObject config, JsArrayString rows, JsArrayString cols, WrapperObject inclusions, String aggregatorName, String rendererName) {
        this.config = config;

        // see convertDataToStrings comment
//        boolean isTable = rendererName != null && rendererName.contains("Table");
//        if(isTable != this.isTable) {
//            this.isTable = isTable;
//
//            this.config = overrideDataClass(this.config, isTable);
//            rerender();
//        }

        List<GPropertyDraw> properties = new ArrayList<>();
        List<GGroupObjectValue> columnKeys = new ArrayList<>();
        List<GPropertyGroupType> types = new ArrayList<>();

        List<String> aggrColumns = createAggrColumns(inclusions);

        fillGroupColumns(rows, properties, columnKeys, types, aggrColumns);
        fillGroupColumns(cols, properties, columnKeys, types, aggrColumns);

        int aggrProps = properties.size();

        for (String aggrColumnCaption : aggrColumns) {
            Column aggrColumn = columnMap.get(aggrColumnCaption);
            properties.add(aggrColumn.property);
            columnKeys.add(aggrColumn.columnKey);
        }

        updateRendererState(true); // will wait until server will answer us if we need to change something
        grid.changeGroupMode(properties, columnKeys, aggrProps, GPropertyGroupType.valueOf(aggregatorName.toUpperCase()));
    }

    private Element rendererElement; // we need to save renderer element, since it is asynchronously replaced, and we might update old element (that is just about to disappear)

    private void setRendererElement(Element element) {
        rendererElement = element;
    }

    protected void updateRendererState(boolean set) {
        updateRendererElementState(rendererElement, set);
    }

    private native void updateRendererElementState(com.google.gwt.dom.client.Element element, boolean set) /*-{
        return $wnd.$(element).find(".pvtRendererArea").css('filter', set ? 'opacity(0.5)' : 'opacity(1)');
    }-*/;

    private native WrapperObject getDefaultConfig(String columnField)/*-{
        var tpl = $wnd.$.pivotUtilities.aggregatorTemplates;
        var instance = this;
        var renderers = $wnd.$.extend(
            $wnd.$.pivotUtilities.subtotal_renderers,
            $wnd.$.pivotUtilities.plotly_renderers,
//            $wnd.$.pivotUtilities.c3_renderers,
//            $wnd.$.pivotUtilities.renderers,
            $wnd.$.pivotUtilities.d3_renderers
        );

        return {
            sorters: {}, // Configuration ordering column for group
            dataClass: $wnd.$.pivotUtilities.SubtotalPivotData,
            cols: [columnField], // inital columns since overwrite is false
            renderers: renderers,
            onRefresh: function (config) {
                instance.@GPivot::onRefresh(*)(config, config.rows, config.cols, config.inclusions, config.aggregatorName, config.rendererName);
            }
        }
    }-*/;

    protected native void render(com.google.gwt.dom.client.Element element, JavaScriptObject array, JavaScriptObject config, JsArrayString orderColumns)/*-{
//        var d = element;
        var d = $doc.createElement('div');
        d.className = 'pvtUiWrapperDiv';

        // Configuration ordering column for group
        config.sorters[@lsfusion.gwt.client.form.object.table.grid.view.GPivot::COLUMN] = $wnd.$.pivotUtilities.sortAs(orderColumns);

        // because we create new element, aggregators every time
        $wnd.$(d).pivotUI(array, config, true);

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

    private static class Record extends JavaScriptObject {

        protected Record() {
        }

        public final native Object get(String column) /*-{
            return this[column];
        }-*/;
    }

    private static class GroupColumnState extends JavaScriptObject {

        protected GroupColumnState() {
        }

        private static native int compare(JavaScriptObject firstArray, JavaScriptObject secondArray) /*-{
            for (var i = 0; i < firstArray.length; i++) {
                if (firstArray[i] > secondArray[i])
                    return 1;
                if (firstArray[i] < secondArray[i])
                    return -1;
            }
            return 0;
        }-*/;

        private native boolean checkLastValue(JsArrayString lastColumns, Record record, boolean desc) /*-{
            if (lastColumns.length === 0)
                return true;
            var lastValues = new Array(lastColumns.length);
            for (var i = 0; i < lastColumns.length; i++)
                lastValues[i] = record[lastColumns[i]];
            var compare = this.lastValues === undefined ? -2 : @GroupColumnState::compare(*)(lastValues, this.lastValues);

            if (compare === -2 || (!desc && compare > 0) || (desc && compare < 0)) {
                this.lastValues = lastValues;
                this.value = null;
            } else {
                if (compare !== 0)
                    return false;
            }
            return true;
        }-*/;

        private native void update(Object addValue, Object aggrFunc) /*-{
            this.value = aggrFunc(this.value, addValue, true);
        }-*/;

        private native Object getValue() /*-{
            return this.value;
        }-*/;
    }

    private static class State extends JavaScriptObject {

        protected State() {
        }

        public native final boolean hasColumnState(String column)/*-{
            var columnState = this[column];
            if (columnState !== undefined && columnState !== null) {
                if (columnState[column].value !== undefined && columnState[column].value !== null) {
                    return true;
                }
            }
            return false;
        }-*/;


        public native final ColumnState getColumnState(String column)/*-{
            var columnState = this[column];
            if (columnState === undefined) {
                columnState = {};
                this[column] = columnState;
            }
            return columnState;
        }-*/;
    }

    private static class ColumnState extends State {

        protected ColumnState() {
        }

        public native final GroupColumnState getGroupState(String column)/*-{
            var groupState = this[column];
            if (groupState === undefined) {
                groupState = {};
                this[column] = groupState;
            }
            return groupState;
        }-*/;
    }

    private static class Aggregator extends JavaScriptObject {

        protected Aggregator() {
        }

        public native static Aggregator create() /*-{
            return {};
        }-*/;

        public native final void setAggregator(String column, ColumnAggregator aggregator)/*-{
            this[column] = aggregator;

            if (this.columns === undefined)
                this.columns = [];
            this.columns.push(column);
        }-*/;

        public native final ColumnAggregator getAggregator(String column)/*-{
            return this[column];
        }-*/;

        public native final JsArrayString getColumns()/*-{
            return this.columns;
        }-*/;

        public native final Object aggr(Object totalAggr, Object oldValue, Object newValue)/*-{
            return totalAggr(oldValue, newValue, false);
        }-*/;

        private void push(State state, Record record, Object defaultAggrFunc) {
            String pushColumn = (String) record.get(COLUMN);
            getAggregator(pushColumn).push(state.getColumnState(pushColumn), record, defaultAggrFunc);
        }

        private Object value(State state, Object totalAggr) {
            JsArrayString columns = getColumns();
            Object result = null;

            for (int i = 0, size = columns.length(); i < size; i++) {
                String column = columns.get(i);

                result = aggr(totalAggr, result, getAggregator(column).value(state.getColumnState(column)));
            }
            return result;
        }

        private String property(State state) {
            JsArrayString columns = getColumns();
            for (int i = 0, size = columns.length(); i < size; i++) {
                if (state.hasColumnState(columns.get(i))) {
                    return columns.get(i);
                }
            }
            return null;
        }
    }

    private final static String[] aggregatorNames = new String[]{"Sum", "Max", "Min"};

    public JavaScriptObject getAggregators(Aggregator aggregator) {
        WrapperObject aggregators = JavaScriptObject.createObject().cast();
        for (String aggregatorName : aggregatorNames)
            aggregators.putValue(aggregatorName, getAggregator(aggregatorName.toUpperCase(), aggregator));
        return aggregators;
    }

    public JavaScriptObject getAggregator(String aggrFuncName, Aggregator aggregator) {
        return getAggregator(aggregator, getValueAggregator(aggrFuncName));
    }

    public native JavaScriptObject getAggregator(Aggregator aggregator, Object aggrFunc) /*-{
        return function () {
            return function () {
                return {
                    aggregator: aggregator,
                    state: {},
                    push: function (record) {
                        aggregator.@Aggregator::push(*)(this.state, record, aggrFunc);
                    },
                    value: function () {
                        return aggregator.@Aggregator::value(*)(this.state, aggrFunc);
                    },
                    numInputs: 0
                }
            }
        }
    }-*/;

    public void renderValueCell(Element jsElement, JavaScriptObject value, JsArrayString rowKey, JsArrayString columnKeys) {
        GPropertyTableBuilder.renderTD(rowHeight, jsElement);
        JsArrayString cols = config.getArrayString("cols");
        for (int i = 0; i < columnKeys.length(); ++i) {
            if (cols.get(i).equals(COLUMN)) {  
                String column = columnKeys.get(i);
                GridCellRenderer<?> renderer = columnMap.get(column).property.getGridCellRenderer();
                renderer.render(jsElement, font, value, false);
                return;
            }
        }
            
        try {
            String numStr = NumberFormat.getDecimalFormat().format(Double.valueOf(value.toString())); 
            jsElement.setPropertyString("textContent", numStr);
        } catch (Exception ignored) {
            jsElement.setPropertyObject("textContent", value);
        }
    }

    public void renderRowAttrCell(Element th, JavaScriptObject value, JsArrayString rowKeyValues, String attrName, Boolean isExpanded, Boolean isArrow) {
        GPropertyTableBuilder.renderTD(rowHeight, th);
        if (isArrow) {
            renderArrow(th, isExpanded);    
        } else {
            if (attrName != null) {
                if (!attrName.equals(COLUMN)) {
                    GridCellRenderer<?> renderer = columnMap.get(attrName).property.getGridCellRenderer();
                    renderer.render(th, font, value, false);
                } else {
                    th.setPropertyObject("textContent", value);
                }
            }
            if (attrName == null) {
                th.setPropertyString("textContent", "Totals");
            }
        }
    }
    
    public void renderColAttrCell(Element jsElement, JavaScriptObject value, JsArrayString colKeyValues, Boolean isSubtotal, Boolean isExpanded, Boolean isArrow) {
        GPropertyTableBuilder.renderTD(rowHeight, jsElement);
        if (isArrow) {
            renderArrow(jsElement, isExpanded);
        } else {
            if (colKeyValues.length() > 0) {
                JsArrayString cols = config.getArrayString("cols");
                String lastCol = cols.get(colKeyValues.length() - 1);
                if (!isSubtotal && lastCol != null && !lastCol.equals(COLUMN)) {
                    GridCellRenderer<?> renderer = columnMap.get(lastCol).property.getGridCellRenderer();
                    renderer.render(jsElement, font, value, false);
                } else {
                    jsElement.setPropertyObject("textContent", value);
                }
            }
            if (colKeyValues.length() == 0) {
                jsElement.setPropertyString("textContent", "Totals");
            }
        }
    } 
    
    public void renderAxisCell(Element jsElement, JavaScriptObject value, String attrName, Boolean isExpanded, Boolean isArrow) {
        GPropertyTableBuilder.renderTD(rowHeight, jsElement);
        if (isArrow) {
            renderArrow(jsElement, isExpanded);
        } else {
            jsElement.setPropertyObject("textContent", value);
        }
    }

    private void renderArrow(Element jsElement, Boolean isExpanded) {
        jsElement.setPropertyString("textContent", getArrow(isExpanded));
    }

    private static String getArrow(Boolean isExpanded) {
        final String arrowCollapsed = " \u25B6 ";
        final String arrowExpanded = " \u25E2 ";

        String arrow = "";
        if (isExpanded != null)
            arrow = isExpanded ? arrowExpanded : arrowCollapsed;
        return arrow;
    }

    public int getColumnWidth(boolean isAttributeColumn, JsArrayString colKeyValues, JsArrayString axisValues, boolean isArrow, int arrowLevels) {
        final int arrowBaseWidth = 15;
        final int defaultTextColumnWidth = 120;
        final int defaultNumberColumnWidth = 50;
        int width = 0;
        if (isArrow) {
            width = arrowBaseWidth + 10 * arrowLevels;
        } else if (isAttributeColumn) {
            JsArrayString cols = config.getArrayString("cols");
            for (int i = 0; i < colKeyValues.length(); ++i) {
                if (cols.get(i).equals(COLUMN)) {
                    String column = colKeyValues.get(i);
                    width = columnMap.get(column).property.getValueWidth(null);
                    break;
                }
            }
            int colsCount = cols.length();
            if (colKeyValues.length() == colsCount && colsCount > 0 && !cols.get(colsCount - 1).equals(COLUMN)) {
                width = Math.max(width, columnMap.get(cols.get(colsCount - 1)).property.getValueWidth(null));
            }
            if (width == 0) {
                width = defaultNumberColumnWidth;
            }
        } else if (axisValues.length() > 0) {
            width = defaultNumberColumnWidth;
            for (int i = 0; i < axisValues.length(); ++i) {
                if (!axisValues.get(i).equals(COLUMN)) {
                    width = Math.max(width, columnMap.get(axisValues.get(i)).property.getValueWidth(null));
                }
            }
        } else {
            width = defaultTextColumnWidth;
        }
        return width;
    } 
    
    private static class ColumnAggregator extends JavaScriptObject {

        protected ColumnAggregator() {
        }

        public final native String getID() /*-{
            return this.id;
        }-*/;

        public final native void setID(String id) /*-{
            this.id = id;
        }-*/;

        // that's pretty tricky because overlay types don't support inheritance
        public final native void push(ColumnState state, Record record, Object defaultAggrFunc) /*-{
            this.pushImpl(state, record, defaultAggrFunc);
        }-*/;

        public final native JavaScriptObject value(ColumnState state) /*-{
            return this.valueImpl(state);
        }-*/;
    }

    private static class FormulaColumnAggregator extends ColumnAggregator {

        protected FormulaColumnAggregator() {
        }

        public static native FormulaColumnAggregator create() /*-{
            return {
                pushImpl: function (state, record, defaultAggrFunc) {
                    return this.@FormulaColumnAggregator::pushImpl(*)(state, record, defaultAggrFunc);
                },
                valueImpl: function (state) {
                    return this.@FormulaColumnAggregator::valueImpl(*)(state);
                }
            }
        }-*/;

        public final native void setOperands(JsArray<ColumnAggregator> operands) /*-{
            this.operands = operands;
        }-*/;

        public final native void setFormula(String formula) /*-{
            this.formula = $wnd.math.compile(formula);
        }-*/;

        public final native JsArray<ColumnAggregator> getOperands() /*-{
            return this.operands;
        }-*/;

        public final native Object evaluateFormula(JsArrayMixed params) /*-{
            var scope = $wnd.createPlainObject(); // we need to create object not from gwt, since it uses different constructor for {} and in math library there is .constructor == Object check for scope
            for (var i = 0; i < params.length; i++) {
                var param = params[i];
                if (param == null)
                    return param;
                scope['$' + (i + 1)] = param;
            }
            return this.formula.evaluate(scope)
        }-*/;

        public final void pushImpl(ColumnState state, Record record, Object defaultAggrFunc) {
            ColumnState aggrState = state.getColumnState(getID());

            JsArray<ColumnAggregator> aggregators = getOperands();
            for (int i = 0, size = aggregators.length(); i < size; i++) {
                ColumnAggregator aggr = aggregators.get(i);
                aggr.push(aggrState, record, defaultAggrFunc);
            }
        }

        public final Object valueImpl(ColumnState state) {
            ColumnState aggrState = state.getColumnState(getID());

            JsArray<ColumnAggregator> aggregators = getOperands();
            JsArrayMixed values = JavaScriptObject.createArray().cast();
            for (int i = 0, size = aggregators.length(); i < size; i++) {
                ColumnAggregator aggr = aggregators.get(i);
                values.push(aggr.value(aggrState));
            }
            return evaluateFormula(values);
        }
    }

    private static class GroupColumnAggregator extends ColumnAggregator {

        protected GroupColumnAggregator() {
        }

        public static native GroupColumnAggregator create() /*-{
            return {
                pushImpl: function (state, record, defaultAggrFunc) {
                    return this.@GroupColumnAggregator::pushImpl(*)(state, record, defaultAggrFunc);
                },
                valueImpl: function (state) {
                    return this.@GroupColumnAggregator::valueImpl(*)(state);
                }
            }
        }-*/;

        public final native void setAggrFunc(Object aggrFunc) /*-{
            this.aggrFunc = aggrFunc;
        }-*/;

        public final native Object getAggrFunc() /*-{
            return this.aggrFunc;
        }-*/;

        public final native void setLast(JsArrayString lastColumns, boolean lastDesc) /*-{
            this.lastColumns = lastColumns;
            this.lastDesc = lastDesc;
        }-*/;

        public final native JsArrayString getLastColumns() /*-{
            return this.lastColumns;
        }-*/;

        public final native boolean getLastDesc() /*-{
            return this.lastDesc;
        }-*/;

        protected final void pushImpl(ColumnState state, Record record, Object defaultAggrFunc) {
            GroupColumnState groupState = state.getGroupState(getID());

            if (!groupState.checkLastValue(getLastColumns(), record, getLastDesc()))
                return;

            Object aggrFunc = getAggrFunc();
            if (aggrFunc == null)
                aggrFunc = defaultAggrFunc;
            groupState.update(record.get(getID()), aggrFunc);
        }

        protected final Object valueImpl(ColumnState state) {
            GroupColumnState groupState = state.getGroupState(getID());

            return groupState.getValue();
        }
    }

    private native static Object getSumAggregator()/*-{
        return function (oldValue, newValue) {
            return oldValue + newValue;
        }
    }-*/;

    private final static Object SUM = getSumAggregator();

    private native static Object getMaxAggregator()/*-{
        return function (oldValue, newValue) {
            return oldValue > newValue ? oldValue : newValue;
        }
    }-*/;

    private final static Object MAX = getMaxAggregator();

    private native static Object getMinAggregator()/*-{
        return function (oldValue, newValue) {
            return oldValue < newValue ? oldValue : newValue;
        }
    }-*/;

    private final static Object MIN = getMinAggregator();

    private native Object getFinalAggregator(Object aggrFunc)/*-{
        return function (oldValue, newValue, parseNew) {
            if (newValue == null)
                return oldValue;
            if (parseNew) {
                newValue = parseFloat(newValue);
                if (isNaN(newValue))
                    return oldValue;
            }
            if (oldValue == null)
                return newValue;
            return aggrFunc(oldValue, newValue);
        }
    }-*/;

    private Object getValueAggregator(String aggrFuncName) {
        Object baseAggrFunc;
        switch (aggrFuncName) {
            case "SUM":
                baseAggrFunc = SUM;
                break;
            case "MAX":
                baseAggrFunc = MAX;
                break;
            case "MIN":
                baseAggrFunc = MIN;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return getFinalAggregator(baseAggrFunc);
    }

    private FormulaColumnAggregator getFormulaAggregator(GPropertyDraw property, GGroupObjectValue columnKey, ColumnAggregator columnAggregator, Map<GPropertyDraw, Map<GGroupObjectValue, ColumnAggregator>> aggregators) {
        FormulaColumnAggregator aggr = FormulaColumnAggregator.create();

        // formula
        aggr.setFormula(property.formula);

        // operands
        JsArray<ColumnAggregator> aggrOperands = JavaScriptObject.createArray().cast();
        aggrOperands.push(columnAggregator);
        for (GPropertyDraw formulaOperand : property.formulaOperands)
            aggrOperands.push(aggregators.get(formulaOperand).get(columnKey));
        aggr.setOperands(aggrOperands);

        return aggr;
    }

    private GroupColumnAggregator getGroupAggregator(GPropertyDraw property, JsArrayString lastColumns) {
        GroupColumnAggregator aggr = GroupColumnAggregator.create();

        // aggr function
        aggr.setAggrFunc(property.aggrFunc != null ? getValueAggregator(property.aggrFunc) : null);

        // last values
        aggr.setLast(lastColumns, property.lastAggrDesc);

        return aggr;
    }

    public native JavaScriptObject getCallbacks() /*-{
        var instance = this;
        
        return {
            valueCellDblClickHandler: function (td, rowKeyValues, colKeyValues) {
                alert("col: " + colKeyValues + ", row: " + rowKeyValues + ", val: " + td.textContent);
            },
            
            rowAttrHeaderDblClickHandler: function (th, rowKeyValues, attrName) {
                alert("row: " + rowKeyValues + ", rowKey: " + attrName + ", val: " + th.textContent);
            },
            
            colAttrHeaderDblClickHandler: function (element, colKeyValues, isSubtotal) {
                alert("col: " + colKeyValues + ", isSubtotal: " + isSubtotal + ", val: " + element.textContent);
            },
            
            axisHeaderDblClickHandler: function (element, attrName) {
                alert("key: " + attrName + ", val: " + element.textContent);
            },
            
            renderValueCell: function (td, value, rowKeyValues, colKeyValues) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::renderValueCell(*)(td, value, rowKeyValues, colKeyValues);
            },

            renderRowAttrHeaderCell: function (th, value, rowKeyValues, attrName, isExpanded, isArrow) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::renderRowAttrCell(*)(th, value, rowKeyValues, attrName, isExpanded, isArrow);
            },

            renderColAttrHeaderCell: function (element, value, colKeyValues, isSubtotal, isExpanded, isArrow) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::renderColAttrCell(*)(element, value, colKeyValues, isSubtotal, isExpanded, isArrow);
            },

            renderAxisHeaderCell: function (element, value, attrName, isExpanded, isArrow) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::renderAxisCell(*)(element, value, attrName, isExpanded, isArrow);
            },
            
            getColumnWidth: function (isAttributeColumn, colKeyValues, axisValues, isArrow, arrowLevels) {
                return instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::getColumnWidth(*)(isAttributeColumn, colKeyValues, axisValues, isArrow, arrowLevels);
            }
        }
    }-*/;
}
