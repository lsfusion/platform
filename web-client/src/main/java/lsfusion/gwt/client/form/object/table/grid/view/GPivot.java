package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.*;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.constants.NumberConstants;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GObjectType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.controller.remote.DeferredRunner;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeColumnValue;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.property.GPivotOptions;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyGroupType;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

import static java.lang.Integer.decode;
import static lsfusion.gwt.client.base.GwtSharedUtils.nullEmpty;
import static lsfusion.gwt.client.base.view.ColorUtils.*;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;
import static lsfusion.gwt.client.view.StyleDefaults.*;

public class GPivot extends GStateTableView implements ColorThemeChangeListener, RenderContext {

    private final String ICON_LEAF = "tree_leaf.png";
    private final String ICON_OPEN = "tree_open.png";
    private final String ICON_CLOSED = "tree_closed.png";
    private final static String ICON_BRANCH = "tree_dots_branch.png";
    private final static String ICON_PASSBY = "tree_dots_passby.png";

    private final String CELL_HEAT_COLOR_ATTRIBUTE_KEY = "data-heat-color";
    private final String CELL_ROW_LEVEL_ATTRIBUTE_KEY = "data-row-level";
    private final String CELL_COLUMN_LEVEL_ATTRIBUTE_KEY = "data-column-level";

    //default values from mainframe.css
    private final static String defaultFontFamily = "Segoe UI";
    private final static int defaultFontSize = 9;

    private GPropertyDraw selectedProperty;

    public GPivot(GFormController formController, GGridController gridController, GPropertyDraw selectedProperty) {
        super(formController, gridController);
        this.selectedProperty = selectedProperty;

        setStyleName(getDrawElement(), "pivotTable");

        MainFrame.addColorThemeChangeListener(this);

        GwtClientUtils.setZeroZIndex(getElement());
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
    private JsArray<JsArrayMixed> getData(NativeStringMap<Column> columnMap, Aggregator aggregator, List<String> aggrCaptions, JsArrayString systemCaptions, boolean convertDataToStrings, boolean full) {
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
            NativeHashMap<GGroupObjectValue, Object> propValues = values.get(i);
            List<NativeHashMap<GGroupObjectValue, Object>> propLastAggrs = lastAggrs.get(i);

            CellRenderer renderer = null;
            if(convertDataToStrings)
                renderer = properties.get(i).getCellRenderer();

            for (GGroupObjectValue columnKey : propColumnKeys) {
                if (checkShowIf(i, columnKey)) // property is hidden
                    continue;

                GGroupObjectValue fullKey = key != null ? GGroupObjectValue.getFullKey(key, columnKey) : GGroupObjectValue.EMPTY;

                pushValue(rowValues, propValues, fullKey, renderer);
                for (NativeHashMap<GGroupObjectValue, Object> propLastAggr : propLastAggrs) {
                    pushValue(rowValues, propLastAggr, fullKey, renderer);
                }
            }
        }
        return rowValues;
    }

    private JsArrayMixed getCaptions(NativeStringMap<Column> columnMap, Aggregator aggregator, List<String> aggrCaptions, JsArrayString systemCaptions) {
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
            NativeHashMap<GGroupObjectValue, Object> propCaptions = captions.get(baseOrder);
            List<NativeHashMap<GGroupObjectValue, Object>> propLastAggrs = lastAggrs.get(baseOrder);

            for (GGroupObjectValue columnKey : propColumnKeys) {
                if(checkShowIf(baseOrder, columnKey))
                    continue;

                String caption = GGridPropertyTable.getPropertyCaption(propCaptions, property, columnKey);

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

    private JavaScriptObject getPropertyCaptionsMap() {
        JavaScriptObject result = JavaScriptObject.createObject();

        for (GPropertyDraw property : properties) {
            int baseOrder = properties.indexOf(property);
            NativeHashMap<GGroupObjectValue, Object> propCaptions = captions.get(baseOrder);

            for (GGroupObjectValue columnKey : columnKeys.get(baseOrder)) {
                if(propCaptions != null) {
                    jsPut(result, property.integrationSID, property.getDynamicCaption(propCaptions.get(columnKey)));
                }
            }
        }
        return result;
    }

    private native void jsPut(JavaScriptObject obj, String key, String value) /*-{
        obj[key] = value;
    }-*/;

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

    private void pushValue(JsArrayMixed rowValues, NativeHashMap<GGroupObjectValue, Object> propValues, GGroupObjectValue fullKey, CellRenderer cellRenderer) {
        Object value = propValues.get(fullKey);
        rowValues.push(value != null ? fromObject(cellRenderer != null ? cellRenderer.format(value) : value) : null);
    }

    public static final String COLUMN = ClientMessages.Instance.get().pivotColumnAttribute();

    private Boolean firstUpdateView = false; // true - default changes applied, false - not yet
    public void setDefaultChangesApplied() {
        firstUpdateView = true;
    }

    @Override
    protected void updateView() {
        columnMap = new NativeStringMap<>();
        aggrCaptions = new ArrayList<>();
        Aggregator aggregator = Aggregator.create();
        JsArrayString systemColumns = JavaScriptObject.createArray().cast();
        boolean convertDataToStrings = false; // so far we'll not use renderer formatters and we'll rely on native toString (if we decide to do it we'll have to track renderer type and rerender everything if this type changes that can may lead to some blinking)
        JsArray<JsArrayMixed> data = getData(columnMap, aggregator, aggrCaptions, systemColumns, convertDataToStrings, true); // convertToObjects()

        if(firstUpdateView != null) // we need to read data first, to know property captions
            initDefaultConfig(grid);

        config = overrideAggregators(config, getAggregators(aggregator), systemColumns);
        config = overrideCallbacks(config, getCallbacks());
        config = overrideRendererOptions(config, getRendererOptions(configFunction, getPropertyCaptionsMap()));

        JsArrayString jsArray = JsArrayString.createArray().cast();
        aggrCaptions.forEach(jsArray::push);

        setStyleName(getDrawElement(), "pivotTable-noSettings", !settings);

        render(getDrawElement(), getPageSizeWidget().getElement(), data, config, jsArray, GwtClientUtils.getCurrentLanguage()); // we need to updateRendererState after it is painted
    }

    public void initDefaultSettings(GGridController gridController) {
        GPivotOptions pivotOptions = gridController.getPivotOptions();
        settings = pivotOptions == null || pivotOptions.isShowSettings();
    }

    private void initDefaultConfig(GGridController gridController) {
        GPivotOptions pivotOptions = gridController.getPivotOptions();
        String rendererName = pivotOptions != null ? pivotOptions.getLocalizedType() : null;
        String aggregationName = pivotOptions != null ? getAggregationName(pivotOptions.getAggregation()) : null;
        configFunction = pivotOptions != null ? pivotOptions.getConfigFunction() : null;

        Map<GPropertyDraw, String> columnCaptionMap = new HashMap<>();
        columnMap.foreachEntry((key, value) -> columnCaptionMap.putIfAbsent(value.property, key));

        List<List<GPropertyDraw>> pivotColumns = gridController.getPivotColumns();
        List<List<GPropertyDraw>> pivotRows = gridController.getPivotRows();
        List<GPropertyDraw> pivotMeasures = gridController.getPivotMeasures();

        if(pivotColumns.isEmpty() && pivotRows.isEmpty() && pivotMeasures.isEmpty() && selectedProperty != null) {
            pivotRows.add(Collections.singletonList(selectedProperty));
        }

        Object[] columns = getPivotCaptions(columnCaptionMap, pivotColumns, COLUMN);
        Integer[] splitCols = getPivotSplits(pivotColumns, COLUMN);

        Object[] rows = getPivotCaptions(columnCaptionMap, pivotRows, null);
        Integer[] splitRows = getPivotSplits(pivotRows, null);

        JsArrayString measures = JavaScriptObject.createArray().cast();
        for(GPropertyDraw property : pivotMeasures) {
            String columnCaption = columnCaptionMap.get(property);
            if(columnCaption != null) {
                measures.push(columnCaption);
            }
        }
        if(measures.length() == 0 && MainFrame.pivotOnlySelectedColumn) {
            for(GPropertyDraw property : properties) {
                if (property.baseType instanceof GIntegralType && (property.sID.equals("PROPERTY(count())") || property.equals(selectedProperty))) {
                    measures.push(columnCaptionMap.get(property));
                }
            }
        }
        WrapperObject inclusions = JavaScriptObject.createObject().cast();
        if(measures.length() > 0) {
            inclusions.putValue(COLUMN, measures);
        }

        JsArrayMixed sortCols = JsArrayString.createArray().cast();
        LinkedHashMap<GPropertyDraw, Boolean> defaultOrders = gridController.getDefaultOrders();
        for(Map.Entry<GPropertyDraw, Boolean> order : defaultOrders.entrySet()) {
            String caption = columnCaptionMap.get(order.getKey());
            if(contains(measures, caption)) {
                sortCols.push(createSortCol(toJsArrayString(caption), order.getValue()));
            } else {
                sortCols.push(createSortCol(caption, order.getValue()));
            }
        }

        config = getDefaultConfig(columns, splitCols, rows, splitRows, inclusions, sortCols, rendererName, aggregationName, settings);
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
    public void runGroupReport() {
        Element plot = getPlotlyChartElement();
        if (plot != null) {
            exportToImage(plot);
        } else {
            exportToExcel(getDrawElement());
        }
    }
    
    public native void exportToImage(Element element) /*-{
        $wnd.Plotly.downloadImage(element, this.@GPivot::getToImageButtonOptions(*)());
    }-*/;
    
    public native JavaScriptObject getToImageButtonOptions() /*-{
        return {format: 'jpeg', filename: 'lsfPlot'};
    }-*/;

    public native void exportToExcel(Element element)
        /*-{
            var instance = this;
            var rootDiv = element.getElementsByClassName("subtotalouterdiv")[0];
            instance.@GPivot::updateTableToExcelAttributes(*)(rootDiv);

            var workbook = $wnd.TableToExcel.tableToBook(rootDiv, {
                sheet: {
                    name: "lsfReport"
                }
            });

            //set column width
            var worksheet = workbook.getWorksheet(1);

            //pin header
            var totalRowLevels = instance.@GPivot::getTotalRowLevels(*)()
            if(totalRowLevels > 0) {
                worksheet.views = [{state: 'frozen', ySplit: totalRowLevels}];
                worksheet.pageSetup.printTitlesRow = '1:' + totalRowLevels;
            }

            worksheet.properties.outlineProperties = {summaryBelow: false};

            $wnd.TableToExcel.save(workbook, "lsfReport.xlsx");
        }-*/;

    private NativeStringMap<Column> columnMap;
    private List<String> aggrCaptions;
    private WrapperObject config;
    private String configFunction;
    private boolean settings = true;

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

    private native WrapperObject overrideRendererOptions(WrapperObject config, JavaScriptObject rendererOptions)/*-{
        return Object.assign({}, config, {
            rendererOptions: rendererOptions
        });
    }-*/;

    private native WrapperObject overrideHideColAxisHeadersColumn(WrapperObject config, boolean hide)/*-{
        return Object.assign({}, config, {
            hideColAxisHeadersColumn: hide
        });
    }-*/;

    private native WrapperObject reduceRows(WrapperObject config, JsArrayString rows, int length)/*-{
        rows = rows.slice(0, length);
        return Object.assign({}, config, {
                rows: rows
            });
    }-*/;

    private native WrapperObject overrideSortCols(WrapperObject config, JsArrayMixed sortCols)/*-{
        return Object.assign({}, config, {
            sortCols: sortCols
        });
    }-*/;

    private static native void remove(JsArrayMixed sortCols, SortCol sortCol) /*-{
        sortCols.splice(sortCols.indexOf(sortCol), 1);
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
        updateSortCols(this.config, config);
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

        //don't reset firstUpdateView if no one column / row / inclusion is visible
        // cols first element is GPivot.COLUMN
        boolean isVisible = cols.length() > 1 || rows.length() > 0 || inclusions.getKeys().length() > 0;

        if(isVisible && (firstUpdateView == null || !firstUpdateView)) { // we don't need to update server groups, since they should be already set
            updateRendererState(true); // will wait until server will answer us if we need to change something
            grid.changeGroups(properties, columnKeys, aggrProps, firstUpdateView != null, getGroupType(aggregatorName.toUpperCase())); // we need to do "changeListViewType" if it's firstUpdateView
            firstUpdateView = null;
        }
    }

    private void afterRefresh() {
        // we don't want to do force-layout, so we'll just emulate UpdateDOMCommand behaviour
        Scheduler.get().scheduleFinally(() -> {
            // is rerendered (so there are new tableDataScroller and header), so we need force Update (and do it after pivot method)
            checkPadding(true);
            restoreScrollLeft();
            setSticky();
        });
    }

    private Element rendererElement; // we need to save renderer element, since it is asynchronously replaced, and we might update old element (that is just about to disappear)

    private void setRendererElement(Element element) {
        rendererElement = element;
    }

    public Element getRendererElement() {
        return rendererElement;
    }

    @Override
    protected Element getRendererAreaElement() {
        return getPivotRendererAreaElement();
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
    private Element getBodyTableScroller() {
        return getElement(rendererElement, ".bodydiv");
    }
    private Element getPivotRendererAreaElement() {
        return getElement(rendererElement, ".pvtRendererArea");
    }
    private Element getPivotRendererElement() {
        return getElement(rendererElement, ".pvtRendererScrollDiv");
    }
    private Element getPlotlyChartElement() {
        return getElement(rendererElement, "div.js-plotly-plot");
    }

    private native NodeList<Element> getElements(com.google.gwt.dom.client.Element element, String selector) /*-{
        return element.querySelectorAll(selector);
    }-*/;

    private native Element getElement(com.google.gwt.dom.client.Element element, String selector) /*-{
        return $wnd.$(element).find(selector).get(0);
    }-*/;

    private String localizeRendererName(JavaScriptObject jsName) {
        String name = jsName.toString();
        return PivotRendererType.valueOf(name).localize();
    }

    private native WrapperObject getDefaultConfig(Object[] columns, Integer[] splitCols, Object[] rows, Integer[] splitRows, JavaScriptObject inclusions, JsArrayMixed sortCols, String rendererName, String aggregatorName, boolean showUI)/*-{
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
            localizeRendererNames($wnd.$.pivotUtilities.plotly_renderers)
//            $wnd.$.pivotUtilities.c3_renderers,
//            $wnd.$.pivotUtilities.renderers,
//            localizeRendererNames($wnd.$.pivotUtilities.d3_renderers)
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
            sortCols: sortCols,
            showUI:showUI,
            valueHeight:@lsfusion.gwt.client.view.StyleDefaults::VALUE_HEIGHT,
            componentHeightString:@lsfusion.gwt.client.view.StyleDefaults::COMPONENT_HEIGHT_STRING,
            cellHorizontalPadding:@lsfusion.gwt.client.view.StyleDefaults::CELL_HORIZONTAL_PADDING,
            columnAttributeName:@lsfusion.gwt.client.form.object.table.grid.view.GPivot::COLUMN,
            toImageButtonOptions: instance.@GPivot::getToImageButtonOptions(*)(),
            onRefresh: function (config) {
                instance.@GPivot::onRefresh(*)(config, config.rows, config.cols, config.inclusions, config.aggregatorName, config.rendererName);
            },
            afterRefresh: function () {
                instance.@GPivot::afterRefresh(*)();
            },
            attach: function () { // we need to add element to dom before rendering to know offsetWidth and offsetHeight for plotly
                // plus that way we avoid blinking on rerendering the whole pivotUI
                var element = instance.@GPivot::getDrawElement()();
                var pivotUIElement = instance.@GPivot::getRendererElement()();
                var existingPivotUIElement = null;
                if(element.hasChildNodes())
                    existingPivotUIElement = element.childNodes[0];
                if(pivotUIElement !== existingPivotUIElement) {
                    if(existingPivotUIElement != null)
                        element.removeChild(existingPivotUIElement);
                    element.appendChild(pivotUIElement);
                }

                var pivotElement = instance.@GPivot::getPivotRendererElement()();
                return { width : pivotElement.offsetWidth, height : pivotElement.offsetHeight };
            },
            getDisplayColor: function (rgb) {
                return @lsfusion.gwt.client.base.view.ColorUtils::getDisplayColor(III)(rgb[0], rgb[1], rgb[2]);
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

        this.@GPivot::setRendererElement(*)(d);

        // it's tricky in pivotUI, first refresh is with timeout 10ms, and that's why there is a blink, when pivotUI is painted with empty Renderer
        // to fix this will add it to the visible DOM in special attach method (which is called just before rendering pivot)
        // also that way we can calculate actual size which is needed for plotly
    }-*/;

    @Override
    public void colorThemeChanged() {
        refreshArrowImages(getElement());
        changePlotColorTheme(getElement());
        updateTableCellsBackground();
    }

    private native void refreshArrowImages(JavaScriptObject pivotElement) /*-{
        var instance = this
        var rootDiv = $wnd.$(pivotElement).find(".subtotalouterdiv").get(0);

        changeImages = function (className, expanded) {
            var imgs = rootDiv.getElementsByClassName(className)
            Array.prototype.forEach.call(imgs, function(img) {
                instance.@GPivot::rerenderArrow(*)(img, expanded)
            });
        }

        changeDots = function (className, branch) {
            var imgs = rootDiv.getElementsByClassName(className)
            Array.prototype.forEach.call(imgs, function(img) {
                instance.@GPivot::rerenderDots(*)(img, branch)
            });
        }

        if (rootDiv !== undefined) {
            changeImages("leaf-image", null)
            changeImages("expanded-image", true)
            changeImages("collapsed-image", false)
            changeDots("branch-image", true)
            changeDots("passby-image", false)
        }
    }-*/;

    private native void changePlotColorTheme(JavaScriptObject pivotElement) /*-{
        $wnd.$.pivotUtilities.colorThemeChanged(this.@GPivot::getPlotlyChartElement(*)());
    }-*/;

    private void updateTableCellsBackground() {
        Element tableHeader = getHeaderTableScroller();
        if (tableHeader != null) {
            NodeList<Element> tds = getElements(tableHeader, ".pvtAxisLabel, .pvtColLabel, .pvtRowLabel, .pvtColLabelFiller, .pvtEmptyHeader");
            for (int i = 0; i < tds.getLength(); i++) {
                setTableToExcelColorAttributes(tds.getItem(i), null);
            }
        }

        Element tableDataScroller = getTableDataScroller();
        if (tableDataScroller != null) {
            NodeList<Element> tds = getElements(tableDataScroller, "td, th");
            for (int i = 0; i < tds.getLength(); i++) {
                Element td = tds.getItem(i);
                String heatColorString = td.getAttribute(CELL_HEAT_COLOR_ATTRIBUTE_KEY);
                if (!GwtSharedUtils.isRedundantString(heatColorString)) {
                    String[] splitColorString = heatColorString.split(",");
                    assert splitColorString.length == 3;
                    try {
                        td.getStyle().setBackgroundColor(getDisplayColor(
                                decode(splitColorString[0]),
                                decode(splitColorString[1]),
                                decode(splitColorString[2])));
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    String rowLevelString = nullEmpty(td.getAttribute(CELL_ROW_LEVEL_ATTRIBUTE_KEY));
                    int rowLevel = rowLevelString != null ? decode(rowLevelString) : -1;
                    String columnLevelString = nullEmpty(td.getAttribute(CELL_COLUMN_LEVEL_ATTRIBUTE_KEY));
                    int columnLevel = columnLevelString != null ? decode(columnLevelString) : -1;
                    setValueCellBackground(td, rowLevel, columnLevel, true);
                }
            }
        }
    }

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

    private static class SortCol extends JavaScriptObject {

        protected SortCol() {
        }

        public final native void init(Object value, boolean direction) /*-{
            this['value'] = value;
            this['direction'] = direction;
        }-*/;

        public final native Object getValue() /*-{
            return this['value'];
        }-*/;

        public final native Boolean getDirection() /*-{
            return this['direction'];
        }-*/;

        public final native void changeDirection() /*-{
                this['direction'] = !this['direction'];
        }-*/;
    }

    private SortCol createSortCol(Object value, boolean direction) {
        SortCol sortCol = JavaScriptObject.createObject().cast();
        sortCol.init(value, direction);
        return sortCol;
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

    private String getColumnName(String attr, JsArrayMixed columnKeys) {
        JsArrayString cols = config.getArrayString(attr);
        for (int i = 0; i < columnKeys.length(); ++i) {
            if (cols.get(i).equals(COLUMN))
                return columnKeys.getString(i);
        }
        return null;
    }

    private String getColumnName(JsArrayMixed rowKeys, JsArrayMixed columnKeys) {
        String column = getColumnName("cols", columnKeys);
        if(column != null)
            return column;

        return getColumnName("rows", rowKeys);
    }

    public void renderValueCell(Element jsElement, JavaScriptObject value, JsArrayMixed rowKeys, JsArrayMixed columnKeys) {
        assert GwtClientUtils.isTDorTH(jsElement);
        GPropertyTableBuilder.renderTD(jsElement);

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

        setValueCellBackground(jsElement, getRowLevel(rowKeys.length()), columnKeys.length(), false);
    }

    public void setValueCellBackground(Element td, int rowLevel, int columnLevel, boolean refresh) {
        int totalRowLevels = getTotalRowLevels();
        int totalColLevels = config.getArrayString("cols").length();
        String cellBackground = null;

        if (totalRowLevels == 0 && (rowLevel == 0 || columnLevel == 0)) {
            cellBackground = getComponentBackground(colorTheme);
        } else {
            int depth = 0;
            if (rowLevel >= 0 && rowLevel < totalRowLevels) {
                depth += totalRowLevels - rowLevel;
            }
            if (columnLevel >= 0 && columnLevel < totalColLevels) {
                depth += totalColLevels - columnLevel;
            }

            if (depth > 0) {
                int[] baseRGB = StyleDefaults.getComponentBackgroundRGB();
                int[] darkenStepRGB = StyleDefaults.getPivotGroupLevelDarkenStepRGB();
                cellBackground = toColorString(
                        Math.min(Math.max(baseRGB[0] + darkenStepRGB[0] * depth, 0), 255),
                        Math.min(Math.max(baseRGB[1] + darkenStepRGB[1] * depth, 1), 255),
                        Math.min(Math.max(baseRGB[2] + darkenStepRGB[2] * depth, 2), 255)
                );
            }
        }

        if (cellBackground != null) {
            td.getStyle().setBackgroundColor(cellBackground);
        }

        if (!refresh) {
            if (rowLevel >= 0) {
                td.setAttribute(CELL_ROW_LEVEL_ATTRIBUTE_KEY, String.valueOf(rowLevel));
            }
            if (columnLevel >= 0) {
                td.setAttribute(CELL_COLUMN_LEVEL_ATTRIBUTE_KEY, String.valueOf(columnLevel));
            }
        }
        setTableToExcelColorAttributes(td, rgbToArgb(cellBackground != null ? cellBackground : getComponentBackground(colorTheme)));
    }

    public void renderRowAttrCell(Element th, JavaScriptObject value, JsArrayMixed rowKeyValues, String attrName, Boolean isExpanded, Boolean isArrow, JsArrayBoolean isLastChildList) {
        assert GwtClientUtils.isTDorTH(th);
        GPropertyTableBuilder.renderTD(th);
        if (isArrow) {
            if (rowKeyValues.length() > 0) {
                int level = getRowLevel(rowKeyValues.length() - 1);
                renderArrow(th, getTreeColumnValue(level, isExpanded, true, false, isLastChildList));
            }
        } else {
            renderAttrCell(th, value, attrName);
        }

        setValueCellBackground(th, getRowLevel(rowKeyValues.length()), -1, false);
    }

    private GTreeColumnValue getTreeColumnValue(int level, Boolean isExpanded, boolean openDotBottom, boolean closedDotBottom, JsArrayBoolean isLastChildList) {
        GTreeColumnValue treeColumnValue = new GTreeColumnValue(level, "level" + level);
        treeColumnValue.setOpen(isExpanded);
        treeColumnValue.setOpenDotBottom(openDotBottom);
        treeColumnValue.setClosedDotBottom(closedDotBottom);

        HashMap<Integer, Boolean> lastInLevelMap = new HashMap<>();
        if(isLastChildList != null) {
            for (int i = 1; i < isLastChildList.length(); i++) {
                lastInLevelMap.put(i - 1, isLastChildList.get(i));
            }
        }
        treeColumnValue.setLastInLevelMap(lastInLevelMap);
        return treeColumnValue;
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
        GPropertyDraw property = columnMap.get(columnName).property;
        GPivot.setTableToExcelPropertyAttributes(th, value, property);

        UpdateContext updateContext = new UpdateContext() {
            @Override
            public boolean globalCaptionIsDrawn() {
                return true;
            }
            @Override
            public void changeProperty(GUserInputResult result) {
            }

            @Override
            public boolean isPropertyReadOnly() {
                return true;
            }

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public boolean isSelectedRow() {
                return false;
            }

            @Override
            public boolean isSelectedLink() {
                return true;
            }
        };
        GPropertyTableBuilder.renderAndUpdate(property, th, this, updateContext);
    }

    @Override
    public boolean globalCaptionIsDrawn() {
        return true;
    }

    @Override
    public GFont getFont() {
        return font;
    }

    public void renderColAttrCell(Element jsElement, JavaScriptObject value, JsArrayMixed colKeyValues, Boolean isSubtotal, Boolean isExpanded, Boolean isArrow) {
        if (isArrow) {
            GPropertyTableBuilder.renderTD(jsElement);
            renderArrow(jsElement, getTreeColumnValue(0, isExpanded, false, false, null));
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

            SortCol sortCol = isSortColumn(isSubtotal, colKeyValues) ? findSortCol(config.getArrayMixed("sortCols"), colKeyValues) : null;
            Boolean sortDir = sortCol != null ? sortCol.getDirection() : null;
            if(lastRenderCol != null && lastRenderCol.equals(COLUMN)) { // value is a column name
                GGridPropertyTableHeader.renderTD(jsElement, true, sortDir, fromObject(value).toString());
                setTableToExcelCenterAlignment(jsElement);
            } else {
                if (isLastCol && sortDir != null) { // last column may have a sortDir
                    jsElement = GwtClientUtils.wrapDiv(jsElement); // we need to wrap jsElement since all other wraps modify upper container

                    jsElement = GwtClientUtils.wrapImg(jsElement, GGridPropertyTableHeader.getSortImgProcesspr(sortDir));
                }

                GPropertyTableBuilder.renderTD(jsElement);
                renderAttrCell(jsElement, value, lastRenderCol);
            }

            if (value != null) {
                jsElement.setTitle(fromObject(value).toString());
            }
        }
        setTableToExcelColorAttributes(jsElement, null);
    }
    
    public void renderAxisCell(Element jsElement, JavaScriptObject value, String attrName, Boolean isExpanded, Boolean isArrow) {
        if (isArrow) {
            GPropertyTableBuilder.renderTD(jsElement);
            Boolean isColumn = attrName.equals(COLUMN);
            int level = isColumn ? 0 : getRowLevel(indexOf(config.getArrayString("rows"), attrName));
            JsArrayBoolean isLastChildList = JsArrayBoolean.createArray().cast();
            for(int i = 0; i <= level; i++) {
                isLastChildList.push(true);
            }
            renderArrow(jsElement, getTreeColumnValue(level, isExpanded, !isColumn, !isColumn, isLastChildList));
        } else {
            SortCol sortCol = findSortCol(config.getArrayMixed("sortCols"), attrName);
            Boolean sortDir = sortCol != null ? sortCol.getDirection() : null;
            // value is a column name, render with rowHeight to make cal attr header to be responsible for the height
            String valueString = fromObject(value).toString();
            GGridPropertyTableHeader.renderTD(jsElement, false, sortDir, valueString);

            if (value != null) {
                jsElement.setTitle(valueString);
            }
        }
        setTableToExcelCenterAlignment(jsElement);
        setTableToExcelColorAttributes(jsElement, null);
    }

    public static void setTableToExcelRowHeight(Element element, Integer rowHeight) {
        element.setAttribute("data-height", String.valueOf(rowHeight * 0.75)); //convert pixels to points
    }

    public static void setTableToExcelPropertyAttributes(Element element, JavaScriptObject value, GPropertyDraw property) {
        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            switch (textAlignStyle) {
                case LEFT:
                    element.setAttribute("data-a-h", "left");
                    break;
                case CENTER:
                    element.setAttribute("data-a-h", "center");
                    break;
                case RIGHT:
                    element.setAttribute("data-a-h", "right");
                    break;
            }
        }

        if(property.font != null) {
            if(property.font.family != null) {
                element.setAttribute("data-f-name", property.font.family);
            }
            if(property.font.size > 0) {
                element.setAttribute("data-f-sz", String.valueOf(property.font.size));
            }
            if(property.font.italic) {
                element.setAttribute("data-f-italic", "true");
            }
            if(property.font.bold) {
                element.setAttribute("data-f-bold", "true");
            }
        }

        //data type and format
        String type;
        String dataValue = null;
        if(property.baseType instanceof GObjectType || property.baseType instanceof GIntegralType) {
            type = "n";
            Object propertyFormat = property.getFormat();
            if(propertyFormat instanceof NumberFormat) {
                String pattern;
                if(value != null) {
                    NumberConstants numberConstants = LocaleInfo.getCurrentLocale().getNumberConstants();
                    dataValue = NumberFormat.getDecimalFormat().format(Double.valueOf(value.toString())).replace(
                            numberConstants.decimalSeparator(), ".").replace(numberConstants.groupingSeparator(), "");
                    BigDecimal numericValue = new BigDecimal(dataValue);
                    int fractDigits = 0;
                    while (numericValue.longValue() - numericValue.doubleValue() != 0) {
                        numericValue = numericValue.multiply(BigDecimal.TEN);
                        fractDigits++;
                    }
                    if (fractDigits > 0) {
                        pattern = "#,##0." + replicate('0', fractDigits);
                    } else {
                        pattern = "#,##0";
                    }
                } else {
                    pattern = ";;;@";
                }
                element.setAttribute("data-num-fmt", pattern);

            }
        } else if(property.baseType instanceof GLogicalType) {
            type = "b";
            dataValue = String.valueOf(value != null);
        } else {
            type = "s";
        }
        element.setAttribute("data-t", type);
        if(dataValue != null) {
            element.setAttribute("data-v", dataValue);
        }
    }

    private static String replicate(char character, int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, character);
        return new String(chars);
    }

    private void setTableToExcelCenterAlignment(Element element) {
        element.setAttribute("data-a-h", "center"); //horizontal alignment
        element.setAttribute("data-a-v", "middle"); //vertical alignment
    }

    private void setTableToExcelColorAttributes(Element element, String backgroundColor) {
        element.setAttribute("data-b-a-s", "thin"); //border
        element.setAttribute("data-b-a-c", rgbToArgb(getGridSeparatorBorderColor(colorTheme))); //border color
        element.setAttribute("data-f-color", rgbToArgb(getTextColor(colorTheme))); //font color
        element.setAttribute("data-fill-color", backgroundColor != null ? backgroundColor : rgbToArgb(getPanelBackground(colorTheme)));
    }

    private void updateTableToExcelAttributes(Element rootDiv) {
        int totalRowLevels = getTotalRowLevels();
        boolean excludeFirstColumn = totalRowLevels > 1;

        //set row height and exclude first column
        NodeList<Element> trs = getElements(rootDiv, "tr");
        for (int i = 0; i < trs.getLength(); i++) {
            Element tr = trs.getItem(i);
            tr.setAttribute("data-height", String.valueOf(getTableToExcelMaxRowHeight(tr)));
            if(excludeFirstColumn) {
                Element firstTH = getElement(tr, "th");
                if (firstTH != null) {
                    firstTH.setAttribute("data-exclude", "true");
                }
            }
        }

        //set outlineLevel
        if (excludeFirstColumn) {
            NodeList<Element> bodyTrs = getElements(getBodyTableScroller(), "tr");
            for (int i = 0; i < bodyTrs.getLength(); i++) {
                Element tr = bodyTrs.getItem(i);
                String rowLevel = nullEmpty(getAttributeRecursive(tr, CELL_ROW_LEVEL_ATTRIBUTE_KEY));
                tr.setAttribute("data-outline-level", String.valueOf((rowLevel != null ? Integer.parseInt(rowLevel) : totalRowLevels) - 1));
            }
        }

        //set horizontal and vertical alignment; font: family, size, italic, bold; border; border color; font color, background color
        NodeList<Element> elements = getElements(rootDiv, ".pvtAxisLabel, .pvtColLabel, .pvtRowLabel, .pvtColLabelFiller, .pvtVal");
        for (int i = 0; i < elements.getLength(); i++) {
            Element th = elements.getItem(i);
            updateAttribute(th, "data-f-name", defaultFontFamily);
            updateAttribute(th, "data-f-sz", String.valueOf(defaultFontSize));
            for(String attribute : new String[]{"data-a-h", "data-a-v", "data-f-italic", "data-f-bold",
                    "data-b-a-s", "data-b-a-c", "data-f-color", "data-fill-color"}) {
                updateAttribute(th, attribute, null);
            }
        }

        //set column width
        Element headerTable = getHeaderTableElement();
        int headerWidth = headerTable.getOffsetWidth();
        int sumWidth = 0;
        List<Integer> columnsWidth = new ArrayList<>();
        NodeList<Element> cols = getElements(headerTable, "col");
        for (int j = excludeFirstColumn ? 1 : 0; j < cols.getLength(); j++) {
            //Calibri 11 is default font for excel sheet
            int width = Integer.parseInt(cols.getItem(j).getStyle().getWidth().replace("px", ""));
            sumWidth += width;
            columnsWidth.add(GFontMetrics.getCharWidthString(new GFont("Calibri", 11, false, false) , width));
        }
        double coef = sumWidth >= headerWidth ? 1 : (double) headerWidth / sumWidth;
        StringBuilder colWidth = new StringBuilder();
        for(Integer w : columnsWidth) {
            colWidth.append((colWidth.length() == 0) ? "" : ",").append(w * coef);
        }
        rootDiv.setAttribute("data-cols-width", colWidth.toString());

        NodeList<Element> pvtEmptyHeaders = getElements(rootDiv, ".pvtEmptyHeader");
        for (int i = 0; i < pvtEmptyHeaders.getLength(); i++) {
            setTableToExcelColorAttributes(pvtEmptyHeaders.getItem(i), null);
        }

        NodeList<Element> rowTotals = getElements(rootDiv, ".rowTotal, .pvtGrandTotal");
        for (int i = 0; i < rowTotals.getLength(); i++) {
            rowTotals.getItem(i).setAttribute("data-a-h", "right");
        }

    }

    private double getTableToExcelMaxRowHeight(Element element) {
        String dataHeight = element.getAttribute("data-height");
        double rowHeight = 0;
        if(dataHeight.isEmpty()) {
            NodeList<Node> children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                rowHeight = Math.max(rowHeight, getTableToExcelMaxRowHeight((Element) children.getItem(i)));
            }
        } else {
            rowHeight = Double.parseDouble(dataHeight);
        }
        return rowHeight;
    }

    private void updateAttribute(Element element, String attribute, String defaultValue) {
        String value = nullEmpty(getAttributeRecursive(element, attribute));
        if(value == null) {
            value = defaultValue;
        }
        if(value != null) {
            element.setAttribute(attribute, value);
        }
    }

    private String getAttributeRecursive(Element element, String attribute) {
        String value = element.getAttribute(attribute);
        if (value.isEmpty()) {
            NodeList<Node> children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.getItem(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    value = getAttributeRecursive((Element) child, attribute);
                    if (!value.isEmpty()) break;
                }
            }
        }
        return value;
    }

    private int getRowLevel(int rowIndex) {
        if(rowIndex >= 0) {
            JsArrayInteger splitRows = config.getArrayInteger("splitRows");
            for(int i = 0; i < splitRows.length(); i++) {
                if(rowIndex <= splitRows.get(i))
                    return i;
            }
        }
        return -1;
    }

    public void renderValue(Element jsElement, JavaScriptObject value) {
//        GPropertyTableBuilder.setLineHeight(jsElement, rowHeight);
        GPropertyTableBuilder.setVerticalMiddleAlign(jsElement);

        jsElement.setPropertyObject("textContent", value);
    }

    private void renderArrow(Element jsElement, GTreeColumnValue treeColumnValue) {
        jsElement.removeAllChildren();
        if (treeColumnValue.getLevel() > 0) {
            jsElement.getStyle().setPaddingLeft(5, Style.Unit.PX);
        }
        GTreeTable.renderExpandDom(jsElement, treeColumnValue);
    }

    private void rerenderArrow(ImageElement img, Boolean isExpanded) {
        GwtClientUtils.setThemeImage(isExpanded == null ? ICON_LEAF : isExpanded ? ICON_OPEN : ICON_CLOSED, img::setSrc);
    }

    private void rerenderDots(ImageElement img, boolean branch) {
        if(branch) {
            GwtClientUtils.setThemeImage(ICON_BRANCH, img::setSrc);
        } else {
            GwtClientUtils.setThemeImage(ICON_PASSBY, str -> img.getStyle().setBackgroundImage("url('" + str + "')"));
        }
    }

    private int getArrowColumnWidth(int arrowLevels) {
        final int arrowBaseWidth = 35;
        return arrowBaseWidth + 15 * arrowLevels;
    }

    private final static int defaultValueWidth = 80;

    private int getValueColumnWidth(JsArrayMixed colValues) {
        int width = 0;
        JsArrayString cols = config.getArrayString("cols");
        for (int i = 0; i < cols.length(); ++i) {
            String column = cols.get(i);
            if (column.equals(COLUMN)) {
                if(i < colValues.length()) {
                    column = colValues.getString(i);
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
        return columnMap.get(column).property.getValueWidthWithPadding(font);
    }

    public int getColumnWidth(boolean isValueColumn, JsArrayMixed colKeyValues, JsArrayString axisValues, boolean isArrow, int arrowLevels) {
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
        resizePlotlyChart();

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

    private Integer scrollLeft = null;
    private int scrollLeftCounter = 0;

    private void saveScrollLeft() {
        Element tableDataScroller = getTableDataScroller();
        if(tableDataScroller != null) {
            scrollLeft = tableDataScroller.getScrollLeft();
            scrollLeftCounter++;
        }

    }

    private void restoreScrollLeft() {
        Element tableDataScroller = getTableDataScroller();
        if(tableDataScroller != null && scrollLeft != null) {
            tableDataScroller.setScrollLeft(scrollLeft);
            if(scrollLeftCounter == 0) {
                scrollLeft = null;
            } else
                scrollLeftCounter--;
        }
    }

    private void setSticky() {
        Element tableHeader = getHeaderTableScroller();
        if (tableHeader != null) {
            NodeList<Element> trs = getElements(tableHeader, "tr");
            for (int i = 0; i < trs.getLength(); i++) {
                setStickyRow(trs.getItem(i), ".pvtEmptyHeader, .pvtAxisLabel", true);
            }
        }

        Element tableBody = getBodyTableScroller();
        if (tableBody != null) {
            NodeList<Element> rows = getElements(tableBody, "tr");
            int rowsCount = rows.getLength();
            for (int i = 0; i < rowsCount; i++) {
                setStickyRow(rows.getItem(i), ".pvtRowLabel", false);
                if (i == rowsCount - 1) {
                    setStickyRow(rows.getItem(rows.getLength() - 1), ".pvtTotalLabel", true);
                }
            }
        }
    }

    private void setStickyRow(Element row, String classes, boolean header) {
        NodeList<Element> cells = getElements(row, classes);
        int left = 0;
        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = cells.getItem(i);
            if(i == 0)
                left = cell.getOffsetLeft();
            cell.addClassName(header ? "pvtStickyHeader" : "pvtStickyCell");
            cell.getStyle().setProperty("left", left + "px");
            left += cell.getOffsetWidth();
        }
    }

    public native void resizePlotlyChart() /*-{
        var plotlyElement = this.@GPivot::getPlotlyChartElement()();
        if (plotlyElement) {
            var pivotElement = this.@GPivot::getPivotRendererElement()();
            var update = $wnd.createPlainObject(); // we need to create object not from gwt, since it uses different constructor for { ... } and in plotly library there is .constructor == Object check for update object
            update["width"] = pivotElement.offsetWidth;
            update["height"] = pivotElement.offsetHeight;

            $wnd.Plotly.relayout(plotlyElement, update);
        }
    }-*/;

    private GroupColumnAggregator getGroupAggregator(GPropertyDraw property, JsArrayString lastColumns) {
        GroupColumnAggregator aggr = GroupColumnAggregator.create();

        // aggr function
        aggr.setAggrFunc(property.aggrFunc != null ? getValueAggregator(property.aggrFunc) : null);

        // last values
        aggr.setLast(lastColumns, property.lastAggrDesc);

        return aggr;
    }

    public native JavaScriptObject getRendererOptions(String configFunction, JavaScriptObject params) /*-{
        return configFunction ? $wnd[configFunction](params) : {}
    }-*/;

    public native JavaScriptObject getCallbacks() /*-{
        var instance = this;
        
        return {
            valueCellDblClickHandler: function (event, td, rowKeyValues, colKeyValues) {
                instance.@GPivot::cellDblClickAction(*)(rowKeyValues, colKeyValues, event.clientX, event.clientY);
            },

            rowAttrHeaderClickHandler: function (event, th, rowKeyValues, attrName) {
                instance.@GPivot::rowAttrHeaderClickAction(*)(rowKeyValues, attrName, isOdd(event.detail));
            },

            colAttrHeaderClickHandler: function (event, element, colKeyValues, isSubtotal) {
                instance.@GPivot::colAttrHeaderClickAction(*)(colKeyValues, element, isSubtotal, event.ctrlKey, event.shiftKey, isOdd(event.detail));
            },
            
            colAxisHeaderDblClickHandler: function (event, element, attrName) {
                //nothing
            },

            rowAxisHeaderDblClickHandler: function (event, element, attrName) {
                instance.@GPivot::rowAxisHeaderDblClickAction(*)(attrName, element, attrName, event.ctrlKey, event.shiftKey);
            },
            
            renderValueCell: function (td, value, rowKeyValues, colKeyValues) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::renderValueCell(*)(td, value, rowKeyValues, colKeyValues);
            },

            renderRowAttrHeaderCell: function (th, value, rowKeyValues, attrName, isExpanded, isArrow, isLastChildList) {
                instance.@lsfusion.gwt.client.form.object.table.grid.view.GPivot::renderRowAttrCell(*)(th, value, rowKeyValues, attrName, isExpanded, isArrow, isLastChildList);
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

        //2 double clicks will be handled as 4 clicks with detail = 1, 2, 3, 4
        function isOdd(num) { return num % 2 === 0; }

    }-*/;

    private void cellDblClickAction(JsArrayMixed rowKeyValues, JsArrayMixed colKeyValues, int x, int y) {
        final PopupDialogPanel popup = new PopupDialogPanel();

        List<String> menuItems = new ArrayList<>();
        JsArrayString cols = config.getArrayString("cols");
        JsArrayString rows = config.getArrayString("rows");
        columnMap.foreachKey(key -> {if(!contains(cols, key) && !contains(rows, key)) menuItems.add(key);});

        final MenuBar menuBar = new MenuBar(true);
        for(String caption : menuItems) {
            MenuItem menuItem = new MenuItem(caption, () -> {
                popup.hide();
                config = reduceRows(config, config.getArrayString("rows"), rowKeyValues.length());

                ArrayList<GPropertyFilter> filters = new ArrayList<>();
                filters.addAll(getFilters(config.getArrayString("rows"), rowKeyValues));
                filters.addAll(getFilters(config.getArrayString("cols"), colKeyValues));

                config.getArrayString("rows").push(caption);
                grid.filter.applyFilters(filters, false);
//                updateView(true, null);
            });
            menuBar.addItem(menuItem);
        }

        GwtClientUtils.showPopupInWindow(popup, menuBar, x, y);
    }


    private List<GPropertyFilter> getFilters(JsArrayString elements, JsArrayMixed values) {
        List<GPropertyFilter> filters = new ArrayList<>();
        for (int i = 0; i < elements.length(); i++) {
            Column column = columnMap.get(elements.get(i));
            if (column != null)
                filters.add(new GPropertyFilter(new GFilter(column.property), grid.groupObject, column.columnKey, getObjectValue(values, i), GCompare.EQUALS));
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

    private int indexOf(JsArrayString array, String element) {
        for (int i = 0; i < array.length(); i++) {
            if (array.get(i).equals(element)) {
                return i;
            }
        }
        return -1;
    }

    private JsArrayString toJsArrayString(String value) {
        JsArrayString array = JavaScriptObject.createArray().cast();
        array.push(value);
        return array;
    }

    private void rowAttrHeaderClickAction(JsArrayMixed rowKeyValues, String attrName, boolean dblClick) {
        if((dblClick || GFormController.isLinkEditMode()) && rowKeyValues.length() > 0) {
            Column column = columnMap.get(attrName);
            Integer rowIndex = getRowIndex(rowKeyValues, false);
            if (column != null && rowIndex != null) {
                executePropertyEditAction(column, rowIndex);
            }
        }
    }

    private long executePropertyEditAction(Column column, Integer rowIndex) {
        return form.syncExecutePropertyEventAction(null, null, column.property, keys.get(rowIndex), GEditBindingMap.EDIT_OBJECT);
    }

    private Integer getRowIndex(JsArrayMixed keyValues, boolean cols) {
        JsArrayString rowsOrCols = config.getArrayString(cols ? "cols" : "rows");
        JsArray<JsArrayMixed> data = getData(columnMap, Aggregator.create(), aggrCaptions, JavaScriptObject.createArray().cast(), false, false);
        ArrayList<String> headers = toArrayList(data.get(0));
        List<Integer> headerIndexes = new ArrayList<>();
        for (int i = 0; i < rowsOrCols.length(); i++) {
            headerIndexes.add(headers.indexOf(rowsOrCols.get(i)));
        }

        Integer rowIndex = 0;
        for (int i = 1; i < data.length(); i++) {
            JsArrayMixed row = data.get(i);
            boolean found = true;
            for (int j = 0; j < keyValues.length(); j++) {
                Integer headerIndex = headerIndexes.get(j);
                if (!isSystemColumn(row, headerIndex) && !equals(getObjectValue(row, headerIndex), getObjectValue(keyValues, j))) {
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

    // should be used instead of JsArrayMixed.getObject since it does some unnecessary convertions
    private Object getObjectValue(JsArrayMixed rowValues, int index) {
        return toObject(getRawObjectValue(rowValues, index));
    }
    private native final JavaScriptObject getRawObjectValue(JsArrayMixed rowValues, int index) /*-{
        return rowValues[index];
    }-*/;

    private boolean isSystemColumn(JsArrayMixed row, Integer headerIndex) {
        return row.length() <= headerIndex;
    }

    private void colAttrHeaderClickAction(JsArrayMixed columnKeyValues, Element th, Boolean isSubtotal, boolean ctrlKey, boolean shiftKey, boolean dblClick) {
        if(dblClick) {
            if (isSortColumn(isSubtotal, columnKeyValues)) {
                saveScrollLeft();
                modifySortCols(columnKeyValues, ctrlKey, shiftKey);
                if (!shiftKey && !ctrlKey) {
                    unwrapOthers(rendererElement, th);
                }
                th.removeAllChildren();
                renderColAttrCell(th, fromObject(getObjectValue(columnKeyValues, columnKeyValues.length() - 1)), columnKeyValues, isSubtotal, false, false);

                //modifySortCols should be rendered immediately, because updateView without DeferredRunner will lead to layout shift
                updateViewLater();
            }
        } else {
            if(GFormController.isLinkEditMode() && columnKeyValues.length() > 0) {
                Column column = columnMap.get(config.getArrayString("cols").get(columnKeyValues.length() - 1));
                Integer rowIndex = getRowIndex(columnKeyValues, true);
                if (column != null && rowIndex != null) {
                    executePropertyEditAction(column, rowIndex);
                }
            }
        }
    }

    private void rowAxisHeaderDblClickAction(String attrName, Element th, String columnCaption, boolean ctrlKey, boolean shiftKey) {
        SortCol sortCol = modifySortCols(attrName, ctrlKey, shiftKey);
        if (!shiftKey && !ctrlKey) {
            unwrapOthers(rendererElement, th);
        }
        th.removeAllChildren();
        GGridPropertyTableHeader.renderTD(th, false, shiftKey ? null : sortCol == null || !sortCol.getDirection(), columnCaption);

        //modifySortCols should be rendered immediately, because updateView without DeferredRunner will lead to layout shift
        updateViewLater();
    }

    private void updateViewLater() {
        DeferredRunner.get().scheduleUpdateView(new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                updateView(true, null);
            }
        });
    }

    private SortCol modifySortCols(Object keys, boolean ctrlKey, boolean shiftKey) {
        JsArrayMixed sortCols = config.getArrayMixed("sortCols");
        if(sortCols == null) {
            sortCols = JsArrayMixed.createArray().cast();
        }

        SortCol sortCol = findSortCol(sortCols, keys);
        if (shiftKey) {
            if(sortCol != null) {
                remove(sortCols, sortCol);
            }
        } else if(ctrlKey) {
            if (sortCol == null) {
                sortCols.push(createSortCol(keys, true));
            } else {
                sortCol.changeDirection();
            }
        } else {
            boolean direction = sortCol != null ? sortCol.getDirection() : false;
            sortCols = JsArrayMixed.createArray().cast();
            sortCols.push(createSortCol(keys, !direction));
        }

        config = overrideSortCols(config, sortCols);
        return sortCol;
    }

    private int getTotalRowLevels() {
        return config.getArrayString("splitRows").length();
    }

    private native void unwrapOthers(Element element, Element currentElement) /*-{
        $wnd.$(element).find(".dataGridHeaderCell-sortimg").each(function () {
            if(!@GPivot::isDescendant(*)(currentElement, this)) {
                this.remove();
            }
        })
    }-*/;

    private static native boolean isDescendant(Element parent, Element child) /*-{
        var node = child.parentNode;
        while (node != null) {
            if (node === parent) {
                return true;
            }
            node = node.parentNode;
        }
        return false;
    }-*/;

    private boolean isSortColumn(boolean isSubtotal, JsArrayMixed colKeyValues) {
        return isSubtotal || colKeyValues.length() == config.getArrayString("cols").length();
    }

    private SortCol findSortCol(JsArrayMixed sortCols, Object value) {
        if(sortCols != null) {
            for (int i = 0; i < sortCols.length(); i++) {
                SortCol sortCol = sortCols.getObject(i);
                if (equals(sortCol.getValue(), value)) {
                    return sortCol;
                }
            }
        }
        return null;
    }

    private boolean equals(Object a, Object b) {
        if(a instanceof JsArrayMixed || b instanceof JsArrayMixed) {
            return a instanceof JsArrayMixed && b instanceof JsArrayMixed && arraysEquals((JsArrayMixed) a, (JsArrayMixed) b);
        } else {
            if(a == null && b == null) return true;
            return a != null && a.equals(b);
        }
    };

    private native boolean arraysEquals(JsArrayMixed a, JsArrayMixed b)/*-{
        if (a === b) return true;
        if (a.length !== b.length) return false;

        for (var i = 0; i < a.length; ++i) {
            if (a[i] !== b[i]) return false;
        }
        return true;
    }-*/;
    
    // Updates the sorting columns list and saves this list in newConfig.
    // currentConfig has actual sorting directions, newConfig has actual rows/cols lists
    // updateSortCols combines this actual data to form the new sorting columns list
    private native void updateSortCols(WrapperObject currentConfig, WrapperObject newConfig) /*-{
        var instance = this
        var sortCols = newConfig.sortCols;
        var newSortCols = [];
        for (var i = 0; i < sortCols.length; ++i) {
            if (typeof sortCols[i].value === 'string') {
                if (newConfig.rows.includes(sortCols[i].value)) {
                    instance.@GPivot::updateDirection(*)(currentConfig, sortCols[i]);
                    newSortCols.push(sortCols[i]);
                }
            } else if (instance.@GPivot::arraysEquals(*)(currentConfig.cols, newConfig.cols)) {
                instance.@GPivot::updateDirection(*)(currentConfig, sortCols[i])
                newSortCols.push(sortCols[i]);
            }
        }
        newConfig.sortCols = newSortCols;
    }-*/;

    private native void updateDirection(WrapperObject currentConfig, SortCol col) /*-{
        var currentSortCol = this.@GPivot::findSortCol(*)(currentConfig.sortCols, col.value)
        if (currentSortCol != null) {
            col.direction = currentSortCol.direction
        }
    }-*/;

    private ArrayList<String> toArrayList(JsArrayMixed jsArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        for(int i = 0; i < jsArray.length(); i++) {
            arrayList.add(jsArray.getString(i));
        }
        return arrayList;
    }

    @Override
    public boolean isDefaultBoxed() {
        return false;
    }
}
