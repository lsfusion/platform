package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.*;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeColumnValue;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.property.GPivotOptions;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyGroupType;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.*;

import static java.lang.Integer.decode;
import static lsfusion.gwt.client.base.GwtSharedUtils.nullEmpty;
import static lsfusion.gwt.client.base.view.ColorUtils.*;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;
import static lsfusion.gwt.client.view.StyleDefaults.*;

public class GPivot extends GStateTableView implements ColorThemeChangeListener {

    private final String ICON_LEAF = "tree_leaf.png";
    private final String ICON_OPEN = "tree_open.png";
    private final String ICON_CLOSED = "tree_closed.png";
    private final static String ICON_BRANCH = "tree_dots_branch.png";
    private final static String ICON_PASSBY = "tree_dots_passby.png";

    private final String CELL_HEAT_COLOR_ATTRIBUTE_KEY = "data-heat-color";
    private final String CELL_ROW_LEVEL_ATTRIBUTE_KEY = "data-row-level";
    private final String CELL_COLUMN_LEVEL_ATTRIBUTE_KEY = "data-column-level";

    public GPivot(GFormController formController, GGridController gridController) {
        super(formController, gridController);

        setStyleName(getDrawElement(), "pivotTable");

        MainFrame.addColorThemeChangeListener(this);
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

            CellRenderer renderer = null;
            if(convertDataToStrings)
                renderer = properties.get(i).getCellRenderer();

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

    private void pushValue(JsArrayMixed rowValues, Map<GGroupObjectValue, Object> propValues, GGroupObjectValue fullKey, CellRenderer cellRenderer) {
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
            rowHeight = Math.max(rowHeight, property.getValueHeightWithPadding(font));
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

        JsArrayMixed sortCols = JsArrayString.createArray().cast();
        LinkedHashMap<GPropertyDraw, Boolean> defaultOrders = gridController.getDefaultOrders();
        for(Map.Entry<GPropertyDraw, Boolean> order : defaultOrders.entrySet()) {
            sortCols.push(createSortCol(columnCaptionMap.get(order.getKey()), order.getValue()));
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
        exportToExcel(getDrawElement());
    }

    public native void exportToExcel(Element element)
        /*-{
            var instance = this;
            var pvtTable = element.getElementsByClassName("subtotalouterdiv")[0];

            instance.@GPivot::updateTableToExcelPvtEmptyHeader(*)(pvtTable);

            //set row height
            Array.from(pvtTable.querySelectorAll("tr")).forEach(function (item) {
                item.setAttribute("data-height", instance.@GPivot::getTableToExcelRowHeight(*)(item));
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
                    var columnValue = column.values[j];
                    if(columnValue != null) {
                        if (columnValue.length > dataMax) {
                            dataMax = columnValue.length;
                        }
                    }
                }
                column.width = (dataMax < 10 ? 10 : dataMax) * 1.2; //1.2 is magic coefficient to better fit width
            }

            $wnd.TableToExcel.save(workbook, "lsfReport.xlsx");
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

        updateRendererState(true); // will wait until server will answer us if we need to change something
        grid.changeGroupMode(properties, columnKeys, aggrProps, getGroupType(aggregatorName.toUpperCase()));
    }

    private void afterRefresh() {
        checkPadding(true); // is rerendered (so there are new tableDataScroller and header), so we need force Update (and do it after pivot method)
        resizePlotlyChart();
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

    private Element getPlotlyChartElement() {
        return getElement(rendererElement, "div.js-plotly-plot");
    }

   private native void updateRendererElementState(com.google.gwt.dom.client.Element element, boolean set) /*-{
        return $wnd.$(element).find(".pvtRendererArea").css('filter', set ? 'opacity(0.5)' : 'opacity(1)');
    }-*/;

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
            onRefresh: function (config) {
                instance.@GPivot::onRefresh(*)(config, config.rows, config.cols, config.inclusions, config.aggregatorName, config.rendererName);
            },
            afterRefresh: function () {
                instance.@GPivot::afterRefresh(*)();
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

    @Override
    public void colorThemeChanged() {
        refreshArrowImages(getElement());
        changePlotColorTheme(getElement());
        updateTableCellsBackground();
    }

    private native void refreshArrowImages(JavaScriptObject pivotElement) /*-{
        var instance = this
        var outerDiv = $wnd.$(pivotElement).find(".subtotalouterdiv").get(0);

        changeImages = function (className, expanded) {
            var imgs = outerDiv.getElementsByClassName(className)
            Array.prototype.forEach.call(imgs, function(img) {
                instance.@GPivot::rerenderArrow(*)(img, expanded)
            });
        }

        changeDots = function (className, branch) {
            var imgs = outerDiv.getElementsByClassName(className)
            Array.prototype.forEach.call(imgs, function(img) {
                instance.@GPivot::rerenderDots(*)(img, branch)
            });
        }

        if (outerDiv !== undefined) {
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
            NodeList<Element> tds = getElements(tableHeader, "td, th");
            for (int i = 0; i < tds.getLength(); i++) {
                setTableToExcelAttributes(tds.getItem(i), true, false);
            }
            updateTableToExcelPvtEmptyHeader(tableHeader);
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

        setValueCellBackground(jsElement, rowKeys.length(), columnKeys.length(), false);
    }

    public void setValueCellBackground(Element td, int rowLevel, int columnLevel, boolean refresh) {
        int totalRowLevels = config.getArrayString("splitRows").length();
        int totalColLevels = config.getArrayString("cols").length();
        String cellBackground = null;

        int depth = 0;
        if (rowLevel == 0 || columnLevel == 0) {
            if (totalRowLevels == 0) {
                cellBackground = getComponentBackground(colorTheme);
            } else {
                depth = Math.max(totalRowLevels + totalColLevels - 1, 1);
            }
        } else {
            if (rowLevel > 0 && rowLevel < totalRowLevels) {
                depth += totalRowLevels - rowLevel;
            }
            if (columnLevel > 0 && columnLevel < totalColLevels) {
                depth += totalColLevels - columnLevel;
            }
        }

        if (depth > 0) {
            int[] baseRGB = StyleDefaults.getComponentBackgroundRGB();
            int[] darkenStepRGB = StyleDefaults.getPivotGroupLevelDarkenStepRGB();
            cellBackground = toColorString(
                    baseRGB[0] + darkenStepRGB[0] * depth,
                    baseRGB[1] + darkenStepRGB[1] * depth,
                    baseRGB[2] + darkenStepRGB[2] * depth
            );
        }

        if (cellBackground != null) {
            td.getStyle().setBackgroundColor(cellBackground);
        }
        td.setAttribute("data-fill-color", rgbToArgb(cellBackground != null ? cellBackground : getComponentBackground(colorTheme))); //for tableToExcel.js

        if (!refresh) {
            if (rowLevel >= 0) {
                td.setAttribute(CELL_ROW_LEVEL_ATTRIBUTE_KEY, String.valueOf(rowLevel));
            }
            if (columnLevel >= 0) {
                td.setAttribute(CELL_COLUMN_LEVEL_ATTRIBUTE_KEY, String.valueOf(columnLevel));
            }
        }
        setTableToExcelAttributes(td, false, false);
    }

    public void renderRowAttrCell(Element th, JavaScriptObject value, JsArrayString rowKeyValues, String attrName, Boolean isExpanded, Boolean isArrow, JsArrayBoolean isLastChildList) {
        GPropertyTableBuilder.renderTD(th, rowHeight);
        if (isArrow) {
            int level = getRowLevel(rowKeyValues.length() - 1);
            renderArrow(th, getTreeColumnValue(level, isExpanded, true, isLastChildList));
        } else {
            renderAttrCell(th, value, attrName);
        }

        setValueCellBackground(th, rowKeyValues.length(), -1, false);
    }

    private GTreeColumnValue getTreeColumnValue(int level, Boolean isExpanded, boolean openDotBottom, JsArrayBoolean isLastChildList) {
        GTreeColumnValue treeColumnValue = new GTreeColumnValue(level, "level" + level);
        treeColumnValue.setOpen(isExpanded);
        treeColumnValue.setOpenDotBottom(openDotBottom);

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
        property.getCellRenderer().render(th, value, new RenderContext() {
            @Override
            public Integer getStaticHeight() {
                return rowHeight;
            }

            @Override
            public GFont getFont() {
                return font;
            }

            @Override
            public void setAlignment() {
                GPivot.setTableToExcelAlignment(th, property);
            }

            @Override
            public void setFont() {
                GPivot.setTableToExcelFontStyle(th, font);
            }
        }, new UpdateContext() {
            @Override
            public boolean isStaticHeight() {
                return true;
            }
        });
    }

    public void renderColAttrCell(Element jsElement, JavaScriptObject value, JsArrayString colKeyValues, Boolean isSubtotal, Boolean isExpanded, Boolean isArrow) {
        boolean center = false;
        if (isArrow) {
            GPropertyTableBuilder.renderTD(jsElement, rowHeight);
            renderArrow(jsElement, getTreeColumnValue(0, isExpanded, false, null));
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
                GGridPropertyTableHeader.renderTD(jsElement, 0, sortDir, fromObject(value).toString());
                center = true;
            } else {
                if (isLastCol && sortDir != null) { // last column may have a sortDir
                    jsElement = GwtClientUtils.wrapDiv(jsElement); // we need to wrap jsElement since all other wraps modify upper container

                    jsElement = GwtClientUtils.wrapImg(jsElement, GGridPropertyTableHeader.getSortImgProcesspr(sortDir));
                }

                GPropertyTableBuilder.renderTD(jsElement, rowHeight);
                renderAttrCell(jsElement, value, lastRenderCol);
            }

            if (value != null) {
                jsElement.setTitle(fromObject(value).toString());
            }
        }
        setTableToExcelAttributes(jsElement, true, center);
    }
    
    public void renderAxisCell(Element jsElement, JavaScriptObject value, String attrName, Boolean isExpanded, Boolean isArrow) {
        if (isArrow) {
            GPropertyTableBuilder.renderTD(jsElement, rowHeight);
            Boolean openDotBottom = !attrName.equals(COLUMN);
            int level = attrName.equals(COLUMN) ? 0 : getRowLevel(indexOf(config.getArrayString("rows"), attrName));
            JsArrayBoolean isLastChildList = JsArrayBoolean.createArray().cast();
            for(int i = 0; i <= level; i++) {
                isLastChildList.push(true);
            }
            renderArrow(jsElement, getTreeColumnValue(level, isExpanded, openDotBottom, isLastChildList));
        } else {
            SortCol sortCol = findSortCol(config.getArrayMixed("sortCols"), attrName);
            Boolean sortDir = sortCol != null ? sortCol.getDirection() : null;
            // value is a column name, render with rowHeight to make cal attr header to be responsible for the height
            String valueString = fromObject(value).toString();
            GGridPropertyTableHeader.renderTD(jsElement, rowHeight, sortDir, valueString);

            if (value != null) {
                jsElement.setTitle(valueString);
            }
        }
        setTableToExcelAttributes(jsElement, true, true);
    }

    private int getTableToExcelRowHeight(Element element) {
        String dataHeight = element.getAttribute("data-height");
        int rowHeight = 0;
        if(dataHeight.isEmpty()) {
            NodeList<Node> children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                rowHeight = Math.max(rowHeight, getTableToExcelRowHeight((Element) children.getItem(i)));
            }
        } else {
            rowHeight = Integer.parseInt(dataHeight);
        }
        return rowHeight;
    }

    public static void setTableToExcelRowHeight(Element element, Integer rowHeight) {
        element.setAttribute("data-height", String.valueOf(rowHeight));
    }

    public static void setTableToExcelAlignment(Element element, GPropertyDraw property) {
        Style style = element.getStyle();

        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            style.setTextAlign(textAlignStyle);
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
    }

    public static void setTableToExcelFontStyle(Element element, GFont font) {
        if(font != null) {
            if (font.family != null) {
                element.setAttribute("data-f-name", font.family);
            }
            if (font.size > 0) {
                element.setAttribute("data-f-sz", String.valueOf(font.size));
            }
            if(font.italic) {
                element.setAttribute("data-f-italic", "true");
            }
            if(font.bold) {
                element.setAttribute("data-f-bold", "true");
            }
        }
    }

    private void setTableToExcelAttributes(Element element, boolean header, boolean center) {
        while(element != null && !element.getNodeName().toLowerCase().matches("th|td")) {
            element = element.getParentElement();
        }
        if(element != null) {
            element.setAttribute("data-b-a-s", "thin"); //border
            element.setAttribute("data-b-a-c", rgbToArgb(getGridSeparatorBorderColor(colorTheme))); //border color
            element.setAttribute("data-f-color", rgbToArgb(getTextColor(colorTheme))); //font color
            if (header) {
                element.setAttribute("data-fill-color", rgbToArgb(getPanelBackground(colorTheme))); //background color
            }
            if (center) {
                element.setAttribute("data-a-h", "center"); //horizontal alignment
                element.setAttribute("data-a-v", "middle"); //vertical alignment
            }
        }
    }

    private void updateTableToExcelPvtEmptyHeader(Element pvtTable) {
        NodeList<Element> tds = getElements(pvtTable, ".pvtEmptyHeader");
        for (int i = 0; i < tds.getLength(); i++) {
            Element td = tds.getItem(i);
            td.setAttribute("data-fill-color", rgbToArgb(getPanelBackground(colorTheme)));
        }
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
        GPropertyTableBuilder.setLineHeight(jsElement, rowHeight);

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
        return columnMap.get(column).property.getValueWidthWithPadding(font);
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

    public native void resizePlotlyChart() /*-{
        var plotlyElement = this.@GPivot::getPlotlyChartElement(*)();
        if (plotlyElement) {
            $wnd.Plotly.relayout(plotlyElement, '');
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

    public native JavaScriptObject getCallbacks() /*-{
        var instance = this;
        
        return {
            valueCellDblClickHandler: function (event, td, rowKeyValues, colKeyValues) {
                instance.@GPivot::cellDblClickAction(*)(rowKeyValues, colKeyValues, event.clientX, event.clientY);
            },
            
            rowAttrHeaderDblClickHandler: function (event, th, rowKeyValues, attrName) {
                instance.@GPivot::rowAttrHeaderDblClickAction(*)(rowKeyValues, attrName);
            },
            
            colAttrHeaderDblClickHandler: function (event, element, colKeyValues, isSubtotal) {
                if(instance.@GPivot::isSortColumn(*)(isSubtotal, colKeyValues)) {
                    instance.@GPivot::colAttrHeaderDblClickAction(*)(colKeyValues, element, isSubtotal, event.ctrlKey, event.shiftKey);
                }
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
    }-*/;

    private void cellDblClickAction(JsArrayString rowKeyValues, JsArrayString colKeyValues, int x, int y) {
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

    private int indexOf(JsArrayString array, String element) {
        for (int i = 0; i < array.length(); i++) {
            if (array.get(i).equals(element)) {
                return i;
            }
        }
        return -1;
    }

    private void rowAttrHeaderDblClickAction(JsArrayString rowKeyValues, String attrName) {
        if(rowKeyValues.length() > 0) {
            Column column = columnMap.get(attrName);
            Integer rowIndex = getRowIndex(rowKeyValues);
            if (column != null && rowIndex != null) {
                form.executeEventAction(column.property, keys.get(rowIndex), GEditBindingMap.EDIT_OBJECT);
            }
        }
    }

    private Integer getRowIndex(JsArrayString rowKeyValues) {
        JsArrayString rows = config.getArrayString("rows");
        JsArray<JsArrayMixed> data = getData(columnMap, Aggregator.create(), aggrCaptions, JavaScriptObject.createArray().cast(), false, false);
        ArrayList<String> headers = toArrayList(data.get(0));
        List<Integer> headerIndexes = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            headerIndexes.add(headers.indexOf(rows.get(i)));
        }

        Integer rowIndex = 0;
        for (int i = 1; i < data.length(); i++) {
            JsArrayMixed row = data.get(i);
            boolean found = true;
            for (int j = 0; j < rowKeyValues.length(); j++) {
                Integer headerIndex = headerIndexes.get(j);
                if (!isSystemColumn(row, headerIndex) && !equals(row.getString(headerIndex), rowKeyValues.get(j))) {
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

    private void colAttrHeaderDblClickAction(JsArrayString columnKeys, Element th, Boolean isSubtotal, boolean ctrlKey, boolean shiftKey) {
        SortCol sortCol = modifySortCols(columnKeys, ctrlKey, shiftKey);
        if (shiftKey) {
            unwrapThis(th);
        } else {
            if(!ctrlKey) {
                unwrapOthers(rendererElement, th);
            }
            if(sortCol != null) {
                changeSortDirImage(th, !sortCol.getDirection());
            } else {
                th.removeAllChildren();
                renderColAttrCell(th, fromObject(columnKeys.get(columnKeys.length() - 1)), columnKeys, isSubtotal, false, false);
            }
        }
        updateView(true, null);
    }

    private void rowAxisHeaderDblClickAction(String attrName, Element th, String columnCaption, boolean ctrlKey, boolean shiftKey) {
        SortCol sortCol = modifySortCols(attrName, ctrlKey, shiftKey);
        if (shiftKey) {
            unwrapThis(th);
        } else {
            if(!ctrlKey) {
                unwrapOthers(rendererElement, th);
            }
            if(sortCol != null) {
                changeSortDirImage(th, !sortCol.getDirection());
            } else {
                th.removeAllChildren();
                GGridPropertyTableHeader.renderTD(th, rowHeight, true, columnCaption);
            }
        }
        updateView(true, null);
    }

    private SortCol modifySortCols(Object keys, boolean ctrlKey, boolean shiftKey) {
        JsArrayMixed sortCols = config.getArrayMixed("sortCols");
        if(sortCols == null) {
            sortCols = JsArrayMixed.createArray().cast();
        }

        SortCol sortCol = findSortCol(sortCols, keys);
        if (shiftKey) {
            remove(sortCols, sortCol);
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

    private native void unwrapThis(Element currentElement) /*-{
        $wnd.$(currentElement).find(".dataGridHeaderCell-sortimg").each(function () {
            this.remove();
        })
    }-*/;

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

    private native void changeSortDirImage(Element element, boolean sortDir) /*-{
        $wnd.$(element).find(".dataGridHeaderCell-sortimg").each(function () {
            @GGridPropertyTableHeader::changeDirection(*)(this, sortDir);
        })
    }-*/;


    private boolean isSortColumn(boolean isSubtotal, JsArrayString colKeyValues) {
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
        if(a instanceof JsArrayMixed && b instanceof JsArrayMixed) {
            return arraysEquals((JsArrayMixed) a, (JsArrayMixed) b);
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
    
    private native void updateSortCols(WrapperObject oldConfig, WrapperObject newConfig) /*-{
        var instance = this
        var sortCols = newConfig.sortCols;
        var newSortCols = [];
        for (var i = 0; i < sortCols.length; ++i) {
            if (typeof sortCols[i].value === 'string') {
                if (newConfig.rows.includes(sortCols[i].value)) {
                    newSortCols.push(sortCols[i]);
                }
            } else if (instance.@GPivot::arraysEquals(*)(oldConfig.cols, newConfig.cols)) {
                newSortCols.push(sortCols[i]);
            }
        }
        newConfig.sortCols = newSortCols;
    }-*/;

    private ArrayList<String> toArrayList(JsArrayMixed jsArray) {
        ArrayList<String> arrayList = new ArrayList<>();
        for(int i = 0; i < jsArray.length(); i++) {
            arrayList.add(jsArray.getString(i));
        }
        return arrayList;
    }
}
