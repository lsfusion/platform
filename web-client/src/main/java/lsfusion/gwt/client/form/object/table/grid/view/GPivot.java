package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.property.GPivotOptions;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyGroupType;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.*;

public class GPivot extends GStateTableView {

    public GPivot(GFormController formController, GGridController gridController) {
        super(formController, gridController);

        setStyleName(getDrawElement(), "pivotTable");
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

    public boolean isGroup() {
        return true;
    }

    @Override
    public GListViewType getViewType() {
        return GListViewType.PIVOT;
    }

    // we need key / value view since pivot
    private JsArray<JsArrayMixed> getData(Map<String, Column> columnMap, Aggregator aggregator, List<String> aggrCaptions, JsArrayString systemCaptions, boolean convertDataToStrings, boolean full) {
        JsArray<JsArrayMixed> array = JavaScriptObject.createArray().cast();

        array.push(getCaptions(columnMap, aggregator, aggrCaptions, systemCaptions));

        // getting values
        for (GGroupObjectValue key : keys != null && !keys.isEmpty()  ? keys : Collections.singleton((GGroupObjectValue) null)) { // can be null if manual update
            JsArrayMixed rowValues = getValues(key, convertDataToStrings);

            if (full) {
                // if there are no columns (there is no response yet, or they are all filtered, we steel need at least one row, because otherwise it breaks a lot of assertions
                // for example in pivotUI, attrValues -> showInDragDrop will not be filled -> to no COLUMN group column added to the dom -> when onRefresh will be called cols will be empty -> config will be overriden with no predefined cols
                for (String aggrCaption : !aggrCaptions.isEmpty() ? aggrCaptions : Collections.singleton((String) null)) { // putting columns to rows
                    JsArrayMixed aggrRowValues = clone(rowValues);
                    aggrRowValues.push(aggrCaption);
                    array.push(aggrRowValues);
                }
            } else {
                array.push(rowValues);
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

    private String getAggregationName(GPropertyGroupType aggregation) {
        if(aggregation != null) {
            switch (aggregation) {
                case SUM:
                    return "Sum";
                case MAX:
                    return "Max";
                case MIN:
                    return "Min";
            }
        }
        return null;
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

    public static final String COLUMN = ClientMessages.Instance.get().pivotColumnAttribute();

    public boolean firstUpdateView = true;
    @Override
    protected void updateView() {
        columnMap = new NativeHashMap<>();
        aggrCaptions = new ArrayList<>();
        Aggregator aggregator = Aggregator.create();
        JsArrayString systemColumns = JavaScriptObject.createArray().cast();
        boolean convertDataToStrings = false; // so far we'll not use renderer formatters and we'll rely on native toString (if we decide to do it we'll have to track renderer type and rerender everything if this type changes that can may lead to some blinking)
        JsArray<JsArrayMixed> data = getData(columnMap, aggregator, aggrCaptions, systemColumns, convertDataToStrings, true); // convertToObjects()
        if(firstUpdateView) {
            initDefaultConfig(grid);
            firstUpdateView = false;
        }
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

        render(getDrawElement(), getPageSizeWidget().getElement(), data, config, jsArray, GwtClientUtils.getCurrentLanguage()); // we need to updateRendererState after it is painted
    }

    private void initDefaultConfig(GGridController gridController) {
        GPivotOptions pivotOptions = gridController.getPivotOptions();
        String rendererName = pivotOptions != null ? pivotOptions.getLocalizedType() : null;
        String aggregationName = pivotOptions != null ? getAggregationName(pivotOptions.getAggregation()) : null;
        settings = pivotOptions == null || pivotOptions.isShowSettings();

        Map<GPropertyDraw, String> columnCaptionMap = new HashMap<>();
        columnMap.foreachEntry((key, value) -> columnCaptionMap.putIfAbsent(value.property, key));

        List<List<GPropertyDraw>> pivotColumns = gridController.getPivotColumns();
        Object[] columns = getPivotCaptions(columnCaptionMap, pivotColumns, COLUMN);
        Integer[] splitCols = getPivotSplits(pivotColumns, COLUMN);

        List<List<GPropertyDraw>> pivotRows = gridController.getPivotRows();
        Object[] rows = getPivotCaptions(columnCaptionMap, pivotRows, null);
        Integer[] splitRows = getPivotSplits(pivotRows, null);

        JsArrayString measures = JavaScriptObject.createArray().cast();
        List<GPropertyDraw> pivotMeasures = gridController.getPivotMeasures();
        for(GPropertyDraw property : pivotMeasures) {
            String columnCaption = columnCaptionMap.get(property);
            if(columnCaption != null) {
                measures.push(columnCaption);
            }
        }
        WrapperObject inclusions = JavaScriptObject.createObject().cast();
        if(measures.length() > 0) {
            inclusions.putValue(COLUMN, measures);
        }

        config = getDefaultConfig(columns, splitCols, rows, splitRows, inclusions, rendererName, aggregationName, settings);
    }

    private Object[] getPivotCaptions( Map<GPropertyDraw, String> columnCaptionMap, List<List<GPropertyDraw>> propertiesList, String defaultElement) {
        List<String> captions = new ArrayList<>();
        if(defaultElement != null) {
            captions.add(defaultElement);
        }
        for (List<GPropertyDraw> propertyList : propertiesList) {
            for (GPropertyDraw property : propertyList) {
                String columnCaption = columnCaptionMap.get(property);
                if(columnCaption != null)
                    captions.add(columnCaption);
            }
        }
        return captions.toArray();
    }

    private Integer[] getPivotSplits(List<List<GPropertyDraw>> propertiesList, String defaultElement) {
        List<Integer> sizes = new ArrayList<>();
        if(defaultElement != null) {
            sizes.add(1);
        }
        for (List<GPropertyDraw> propertyList : propertiesList)
            if (!propertyList.isEmpty())
                sizes.add(propertyList.size());

        Integer[] splits = new Integer[propertiesList.size()];
        int count = -1;
        for(int i = 0; i < sizes.size(); i++) {
            count += sizes.get(i);
            splits[i] = count;
        }

        return splits;
    }


    @Override
    public void runGroupReport(boolean toExcel) {
        Element drawElement = getDrawElement();
        if (toExcel) {
            exportToExcel(drawElement);
        } else {
            exportToPdf(drawElement);
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

    private NativeHashMap<String, Column> columnMap;
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

        setStyleName(getDrawElement(), "pivotTable-noSettings", !settings);

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

    private native WrapperObject reduceRows(WrapperObject config, JsArrayString rows, int length)/*-{
        rows = rows.slice(0, length);
        return Object.assign({}, config, {
                rows: rows
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
        grid.changeGroupMode(properties, columnKeys, aggrProps, getGroupType(aggregatorName.toUpperCase()));
    }

    private void afterRefresh() {
        checkPadding(true); // is rerendered (so there are new tableDataScroller and header), so we need force Update (and do it after pivot method)
    }

    private Element rendererElement; // we need to save renderer element, since it is asynchronously replaced, and we might update old element (that is just about to disappear)

    private void setRendererElements(Element element) {
        rendererElement = element;
    }

    protected void updateRendererState(boolean set) {
        updateRendererElementState(rendererElement, set);
    }
    private Element getTableDataScroller() {
        return getElement(rendererElement, ".scrolldiv");
    }
    private Element getHeaderTableElement() {
        return getElement(rendererElement, ".headertable.pvtTable");
    }
    private Element getHeaderTableScroller() {
        return getElement(rendererElement, ".headerdiv");
    }

    private native void updateRendererElementState(com.google.gwt.dom.client.Element element, boolean set) /*-{
        return $wnd.$(element).find(".pvtRendererArea").css('filter', set ? 'opacity(0.5)' : 'opacity(1)');
    }-*/;

    private native Element getElement(com.google.gwt.dom.client.Element element, String selector) /*-{
        return $wnd.$(element).find(selector).get(0);
    }-*/;

    private String localizeRendererName(JavaScriptObject jsName) {
        String name = jsName.toString();
        return PivotRendererType.valueOf(name).localize();
    }

    private native WrapperObject getDefaultConfig(Object[] columns, Integer[] splitCols, Object[] rows, Integer[] splitRows, JavaScriptObject inclusions, String rendererName, String aggregatorName, boolean showUI)/*-{
        var instance = this;
        var localizeRendererNames = function(renderers) {
            var localizedRenderers = {};
            for (var key in renderers) {
                if (renderers.hasOwnProperty(key)) {
                    localizedRenderers[instance.@GPivot::localizeRendererName(*)(key)] = renderers[key];
                }
            }
            return localizedRenderers;
        }
        var renderers = $wnd.$.extend(
            localizeRendererNames($wnd.$.pivotUtilities.subtotal_renderers),
            localizeRendererNames($wnd.$.pivotUtilities.plotly_renderers),
//            $wnd.$.pivotUtilities.c3_renderers,
//            $wnd.$.pivotUtilities.renderers,
            localizeRendererNames($wnd.$.pivotUtilities.d3_renderers)
        );

        return {
            sorters: {}, // Configuration ordering column for group
            dataClass: $wnd.$.pivotUtilities.SubtotalPivotData,
            cols: columns, // inital columns since overwrite is false
            splitCols: splitCols,
            rows: rows, // inital rows since overwrite is false
            splitRows: splitRows,
            renderers: renderers,
            rendererName: rendererName,
            aggregatorName: aggregatorName,
            inclusions: inclusions,
            showUI:showUI,
            valueHeight:@lsfusion.gwt.client.view.StyleDefaults::VALUE_HEIGHT,
            componentHeightString:@lsfusion.gwt.client.view.StyleDefaults::COMPONENT_HEIGHT_STRING,
            cellHorizontalPadding:@lsfusion.gwt.client.view.StyleDefaults::CELL_HORIZONTAL_PADDING,
            onRefresh: function (config) {
                instance.@GPivot::onRefresh(*)(config, config.rows, config.cols, config.inclusions, config.aggregatorName, config.rendererName);
            },
            afterRefresh: function () {
                instance.@GPivot::afterRefresh(*)();
            }
        }
    }-*/;

    protected native void render(com.google.gwt.dom.client.Element element, com.google.gwt.dom.client.Element pageSizeElement, JavaScriptObject array, JavaScriptObject config, JsArrayString orderColumns, String language)/*-{
//        var d = element;
        var d = $doc.createElement('div'); // we need some div to append it later to avoid blinking
        d.className = 'pvtUiWrapperDiv';

        // Configuration ordering column for group
        config.sorters[@lsfusion.gwt.client.form.object.table.grid.view.GPivot::COLUMN] = $wnd.$.pivotUtilities.sortAs(orderColumns);

        // because we create new element, aggregators every time
        $wnd.$(d).pivotUI(array, config, true, language);

        // moving pagesize controller inside
        $wnd.$(d).find(".pvtRendererFooter").append(pageSizeElement);

        this.@GPivot::setRendererElements(*)(d);

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
            return { columns : [] };
        }-*/;

        public native final void setAggregator(String column, ColumnAggregator aggregator)/*-{
            this[column] = aggregator;
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
            if(pushColumn != null) // it can be null when there are no columns (see getData method)
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
    }

    private final static String[] aggregatorNames = new String[]{"SUM", "MAX", "MIN"};

    public JavaScriptObject getAggregators(Aggregator aggregator) {
        WrapperObject aggregators = JavaScriptObject.createObject().cast();
        for (String aggregatorName : aggregatorNames)
            aggregators.putValue(getAggregatorName(aggregatorName), getAggregator(aggregatorName, aggregator));
        return aggregators;
    }

    private String getAggregatorName(String aggregatorName) {
        ClientMessages messages = ClientMessages.Instance.get();
        switch (aggregatorName) {
            case "SUM": return messages.pivotAggregatorSum();
            case "MAX": return messages.pivotAggregatorMax();
            case "MIN": return messages.pivotAggregatorMin();
        }
        return "";
    }

    private GPropertyGroupType getGroupType(String aggregatorName) {
        ClientMessages messages = ClientMessages.Instance.get();
        if (aggregatorName.equals(messages.pivotAggregatorSum())) {
            return GPropertyGroupType.SUM;
        } else if (aggregatorName.equals(messages.pivotAggregatorMax())) {
            return GPropertyGroupType.MAX;
        } else if (aggregatorName.equals(messages.pivotAggregatorMin())) {
            return GPropertyGroupType.MIN;
        }
        return GPropertyGroupType.SUM;
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

    private String getColumnName(String attr, JsArrayString columnKeys) {
        JsArrayString cols = config.getArrayString(attr);
        for (int i = 0; i < columnKeys.length(); ++i) {
            if (cols.get(i).equals(COLUMN))
                return columnKeys.get(i);
        }
        return null;
    }

    private String getColumnName(JsArrayString rowKeys, JsArrayString columnKeys) {
        String column = getColumnName("cols", columnKeys);
        if(column != null)
            return column;

        return getColumnName("rows", rowKeys);
    }

    public void renderValueCell(Element jsElement, JavaScriptObject value, JsArrayString rowKeys, JsArrayString columnKeys) {
        GPropertyTableBuilder.renderTD(jsElement, rowHeight);

        String column = getColumnName(rowKeys, columnKeys);
        if(column != null)
            renderColumn(jsElement, value, column);
        else {
            try {
                value = fromString(NumberFormat.getDecimalFormat().format(Double.valueOf(value.toString())));
            } catch (Exception ignored) {
            }
            // value is aggregator result
            renderValue(jsElement, value);
        }
    }

    public void renderRowAttrCell(Element th, JavaScriptObject value, JsArrayString rowKeyValues, String attrName, Boolean isExpanded, Boolean isArrow) {
        GPropertyTableBuilder.renderTD(th, rowHeight);
        if (isArrow) {
            renderArrow(th, isExpanded);    
        } else {
            renderAttrCell(th, value, attrName);
        }
    }

    public void renderAttrCell(Element th, JavaScriptObject value, String columnName) {
        if (columnName != null && !columnName.equals(COLUMN)) {
            renderColumn(th, value, columnName);
        } else {
            // value is either empty (i.e total is rendered) or name of the column
            renderValue(th, value);
        }
    }

    private void renderColumn(Element th, JavaScriptObject value, String columnName) {
        GridCellRenderer<?> renderer = columnMap.get(columnName).property.getGridCellRenderer();
        renderer.render(th, font, value, false);
    }

    public void renderColAttrCell(Element jsElement, JavaScriptObject value, JsArrayString colKeyValues, Boolean isSubtotal, Boolean isExpanded, Boolean isArrow) {
        if (isArrow) {
            GPropertyTableBuilder.renderTD(jsElement, rowHeight);
            renderArrow(jsElement, isExpanded);
        } else {
            isSubtotal = isSubtotal || colKeyValues.length() == 0; // just in case, because in theory when there are no col keys it should be a total

            boolean isLastCol;
            String lastRenderCol;
            if (isSubtotal) {
                lastRenderCol = null;
                isLastCol = true;
            } else {
                JsArrayString cols = config.getArrayString("cols");
                int colSize = colKeyValues.length();
                lastRenderCol = cols.get(colSize - 1);
                isLastCol = colSize == cols.length();
            }

            Boolean sortDir = null;
            if(lastRenderCol != null && lastRenderCol.equals(COLUMN)) { // value is a column name
                GGridPropertyTableHeader.renderTD(jsElement, 0, sortDir, fromObject(value).toString());
            } else {
                if (isLastCol && sortDir != null) { // last column may have a sortDir
                    jsElement = GGridPropertyTableHeader.wrapDiv(jsElement); // we need to wrap jsElement since all other wraps modify upper container

                    jsElement = GGridPropertyTableHeader.wrapSort(jsElement, sortDir);
                }

                GPropertyTableBuilder.renderTD(jsElement, rowHeight);
                renderAttrCell(jsElement, value, lastRenderCol);
            }
        }
    } 
    
    public void renderAxisCell(Element jsElement, JavaScriptObject value, String attrName, Boolean isExpanded, Boolean isArrow) {
        if (isArrow) {
            GPropertyTableBuilder.renderTD(jsElement, rowHeight);
            renderArrow(jsElement, isExpanded);
        } else {
            // value is a column name, render with rowHeight to make cal attr header to be responsible for the height
            GGridPropertyTableHeader.renderTD(jsElement, rowHeight, null, fromObject(value).toString());
        }
    }

    public void renderValue(Element jsElement, JavaScriptObject value) {
        jsElement.setPropertyObject("textContent", value);
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

    private int getArrowColumnWidth(int arrowLevels) {
        final int arrowBaseWidth = 15;
        return arrowBaseWidth + 10 * arrowLevels;
    }

    private final static int defaultValueWidth = 80;

    private int getValueColumnWidth(JsArrayString colValues) {
        int width = 0;
        JsArrayString cols = config.getArrayString("cols");
        for (int i = 0; i < cols.length(); ++i) {
            String column = cols.get(i);
            if (column.equals(COLUMN)) {
                if(i < colValues.length()) {
                    column = colValues.get(i);
                    if(column == null) // it can be null when there are no columns (see getData method)
                        continue;
                } else
                    continue;
            }
            width = Math.max(width, getColumnMapWidth(column));
        }
        return width == 0 ? defaultValueWidth : width;
    }

    private int getAttrColumnWidth(JsArrayString cols) {
        int width = 0;
        for (int i = 0; i < cols.length(); ++i) {
            String column = cols.get(i);
            if (!column.equals(COLUMN)) {
                width = Math.max(width, getColumnMapWidth(column));
            }
        }
        return width == 0 ? defaultValueWidth : width;
    }

    private int getColumnMapWidth(String column) {
        return columnMap.get(column).property.getValueWidth(null);
    }

    public int getColumnWidth(boolean isValueColumn, JsArrayString colKeyValues, JsArrayString axisValues, boolean isArrow, int arrowLevels) {
        if (isArrow) {
            return getArrowColumnWidth(arrowLevels);
        } else if (isValueColumn) {
            return getValueColumnWidth(colKeyValues);
        } else if (axisValues.length() > 0) {
            return getAttrColumnWidth(axisValues);
        }
        return defaultValueWidth;
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

    private boolean hasVerticalScroll;

    @Override
    public void onResize() {
        checkPadding(false);

        super.onResize();
    }

    public void checkPadding(boolean forceUpdate) {
        Element tableDataScroller = getTableDataScroller();
        if(tableDataScroller != null) {
            int scrollWidth = tableDataScroller.getClientWidth();
            boolean newHasVerticalScroll = scrollWidth != tableDataScroller.getOffsetWidth();

            if (forceUpdate || hasVerticalScroll != newHasVerticalScroll) {
                hasVerticalScroll = newHasVerticalScroll;

//                DataGrid.updateTableMargin(hasVerticalScroll, getHeaderTableScroller());
                DataGrid.updateTablePadding(hasVerticalScroll, getHeaderTableElement());
                DataGrid.updateTableRightOuterBorder(hasVerticalScroll, tableDataScroller);
            }
        }
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
            valueCellDblClickHandler: function (td, rowKeyValues, colKeyValues, x, y) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::cellDblClickAction(*)(rowKeyValues, colKeyValues, x, y);
            },
            
            rowAttrHeaderDblClickHandler: function (th, rowKeyValues, attrName) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::attrHeaderDblClickAction(*)(rowKeyValues, true, th.textContent);
            },
            
            colAttrHeaderDblClickHandler: function (element, colKeyValues, isSubtotal) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::attrHeaderDblClickAction(*)(colKeyValues, false, element.textContent);
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
                return instance.@GPivot::getColumnWidth(*)(isAttributeColumn, colKeyValues, axisValues, isArrow, arrowLevels);
            },

            checkPadding: function() {
                return instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::checkPadding(*)(false);
            }
        }
    }-*/;

    private void cellDblClickAction(JsArrayString rowKeyValues, JsArrayString colKeyValues, int x, int y) {
        final PopupPanel popup = new PopupPanel(true);

        List<String> menuItems = new ArrayList<>();
        JsArrayString cols = config.getArrayString("cols");
        JsArrayString rows = config.getArrayString("rows");
        columnMap.foreachKey(key -> {if(!contains(cols, key) && !contains(rows, key)) menuItems.add(key);});

        final MenuBar menuBar = new MenuBar(true);
        for(String caption : menuItems) {
            MenuItem menuItem = new MenuItem(caption, () -> {
                popup.hide();
                config = reduceRows(config, config.getArrayString("rows"), rowKeyValues.length());

                List<GPropertyFilter> filters = new ArrayList<>();
                filters.addAll(getFilters(config.getArrayString("rows"), rowKeyValues));
                filters.addAll(getFilters(config.getArrayString("cols"), colKeyValues));

                config.getArrayString("rows").push(caption);
                grid.filter.applyFilters(filters, false);
                updateView(true, null);
            });
            menuBar.addItem(menuItem);
        }

        popup.setWidget(menuBar);
        GwtClientUtils.showPopupInWindow(popup, x, y);
        Scheduler.get().scheduleDeferred(menuBar::focus);
    }


    private List<GPropertyFilter> getFilters(JsArrayString elements, JsArrayString values) {
        List<GPropertyFilter> filters = new ArrayList<>();
        for (int i = 0; i < elements.length(); i++) {
            Column column = columnMap.get(elements.get(i));
            if (column != null) {
                GPropertyFilter filter = new GPropertyFilter();
                filter.property = column.property;
                GDataFilterValue filterValue = new GDataFilterValue();
                filterValue.value = values.get(i);
                filter.value = filterValue;
                filter.compare = GCompare.EQUALS;
                filters.add(filter);
            }
        }
        return filters;
    }

    private boolean contains(JsArrayString array, String element) {
        for (int i = 0; i < array.length(); i++) {
            if (array.get(i).equals(element)) {
                return true;
            }
        }
        return false;
    }

    private void attrHeaderDblClickAction(JsArrayString keysJsArray, boolean isRows, String value) {
        List<String> keysArray = toArrayList(keysJsArray);
        List<String> rowsOrCols = toArrayList(config.getArrayString(isRows ? "rows" : "cols"));
        String selectedColumn = rowsOrCols.get(keysArray.indexOf(value));
        if (selectedColumn != null) {
            Column column = columnMap.get(selectedColumn);
            Integer rowIndex = getRowIndex(keysArray, rowsOrCols);
            if (column != null && rowIndex != null) {
                form.executeEventAction(column.property, keys.get(rowIndex), GEditBindingMap.EDIT_OBJECT);
            }
        }
    }

    private Integer getRowIndex(List<String> keysArray, List<String> rowsOrCols) {
        JsArray<JsArrayMixed> data = getData(columnMap, Aggregator.create(), aggrCaptions, JavaScriptObject.createArray().cast(), false, false);
        ArrayList<String> headers = toArrayList(data.get(0));
        List<Integer> headerIndexes = new ArrayList<>();
        for (String rowOrCol : rowsOrCols) {
            headerIndexes.add(headers.indexOf(rowOrCol));
        }

        Integer rowIndex = 0;
        for (int i = 1; i < data.length(); i++) {
            JsArrayMixed row = data.get(i);
            boolean found = true;
            for (int j = 0; j < keysArray.size(); j++) {
                Integer headerIndex = headerIndexes.get(j);
                if (!isSystemColumn(row, headerIndex) && !row.getString(headerIndex).equals(keysArray.get(j))) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return rowIndex;
            }
            rowIndex++;
        }
        return null;
    }

    private boolean isSystemColumn(JsArrayMixed row, Integer headerIndex) {
        return row.length() <= headerIndex;
    }

    private ArrayList<String> toArrayList(JsArrayMixed jsArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        for(int i = 0; i < jsArray.length(); i++) {
            arrayList.add(jsArray.getString(i));
        }
        return arrayList;
    }

    private ArrayList<String> toArrayList(JsArrayString jsArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        for(int i = 0; i < jsArray.length(); i++) {
            arrayList.add(jsArray.get(i));
        }
        return arrayList;
    }
}
