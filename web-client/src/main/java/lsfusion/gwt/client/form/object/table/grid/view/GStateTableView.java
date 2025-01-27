package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

// view with state, without incremental updates
// flexpanel, since we need to add pagesize widget + attach it to handle events
public abstract class GStateTableView extends FlexPanel implements GTableView {

    protected final GFormController form;
    protected final GGridController grid;

    protected GFont font;

    protected GGroupObjectValue currentKey;

    private long setRequestIndex;

    protected List<GGroupObjectValue> keys;

    protected List<GPropertyDraw> properties = new ArrayList<>();
    protected List<List<GGroupObjectValue>> columnKeys = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> captions = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> captionElementClasses = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> values = new ArrayList<>();
    protected List<List<NativeHashMap<GGroupObjectValue, PValue>>> lastAggrs = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> readOnlys = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> showIfs = new ArrayList<>();
    protected NativeHashMap<GGroupObjectValue, PValue> rowBackgroundValues = new NativeHashMap<>();
    protected NativeHashMap<GGroupObjectValue, PValue> rowForegroundValues = new NativeHashMap<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> cellGridElementClasses = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> cellValueElementClasses = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> cellFontValues = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> cellBackgroundValues = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> cellForegroundValues = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> placeholders = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> patterns = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> regexps = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> regexpMessages = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> tooltips = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, PValue>> valueTooltips = new ArrayList<>();

    protected boolean checkShowIf(int property, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> propertyShowIfs = showIfs.get(property);
        return propertyShowIfs != null && propertyShowIfs.get(columnKey) == null;
    }

    public final native JsArrayMixed clone(JsArrayMixed array) /*-{
        n = array.length;
        var clone = [];
        for(var i = 0; i < n; i++) {
            clone.push(array[i]);
        }
        return clone;
    }-*/;

    private final TableContainer tableContainer;

    public GStateTableView(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(true);

        this.form = form;
        this.grid = grid;

        this.tableContainer = tableContainer;

//        setElement(DOM.createDiv());

        rerender = true;

        drawWidget = new DivWidget();
        addFill(drawWidget);

        initPageSizeWidget();

        GFormController.setBindingGroupObject(this, grid.groupObject);
    }

    private final Label messageLabel = new Label();

    public void initPageSizeWidget() {
        FlexPanel messageAndButton = new FlexPanel();
        messageLabel.getElement().getStyle().setPaddingRight(4, Style.Unit.PX);
        messageAndButton.addCentered(messageLabel);

        StaticImageButton showAllButton = new StaticImageButton(ClientMessages.Instance.get().formGridPageSizeShowAll(), null);
        showAllButton.addClickHandler(event -> {
            pageSize = Integer.MAX_VALUE / 10; // /10 to prevent Integer overflow because in GroupObjectInstance we use "pageSize * 2"
            this.grid.changePageSize(pageSize);
        });
        messageAndButton.addCentered(showAllButton);

        FlexPanel centeredMessageAndButton = new FlexPanel(true);
        centeredMessageAndButton.addCentered(messageAndButton);
        centeredMessageAndButton.getElement().getStyle().setPadding(2, Style.Unit.PX);

        this.pageSizeWidget = centeredMessageAndButton;
        this.pageSizeWidget.setVisible(false);

        ResizableSimplePanel child = new ResizableSimplePanel();
        child.setWidget(this.pageSizeWidget);
        addStretched(child); // we need to attach pageSize widget to make it work
//
//        add(new ResizableSimplePanel(this.pageSizeWidget)); // we need to attach pageSize widget to make it work
    }

    private final Widget drawWidget;
    public Element getDrawElement() {
        return drawWidget.getElement();
    }

    public Widget getPopupOwnerWidget() {
        return getWidget();
    }

    public Element getTableDataFocusElement() {
        return tableContainer.getFocusElement();
    }

    private Widget pageSizeWidget;
    protected Widget getPageSizeWidget() {
        return pageSizeWidget;
    }

    protected boolean dataUpdated = false;

    @Override
    public void setCurrentKey(GGroupObjectValue currentKey) {
        setCurrentKey(currentKey, true);
    }

    private void setCurrentKey(GGroupObjectValue currentKey, boolean rendered) {
        this.currentKey = currentKey;
        if (!rendered)
            dataUpdated = true;
    }

    private int pageSize = getDefaultPageSize();

    // should correspond FormInstance.constructor - changePageSize method
    public int getDefaultPageSize() {
        return 1000;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    protected boolean isPageSizeHit() {
        return keys != null && keys.size() >= getPageSize();
    }

    @Override
    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.keys = keys;

        dataUpdated = true;
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
        int index = properties.indexOf(property);
        if(!updateKeys) {
            if(index < 0) {
                index = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), this.properties);

                this.captions.add(index, null);
                this.properties.add(index, property);
                this.columnKeys.add(index, null);
                this.values.add(index, null);
                this.readOnlys.add(index, null);
                this.showIfs.add(index, null);
                this.cellGridElementClasses.add(index, null);
                this.cellValueElementClasses.add(index, null);
                this.cellFontValues.add(index, null);
                this.cellBackgroundValues.add(index, null);
                this.cellForegroundValues.add(index, null);
                this.placeholders.add(index, null);
                this.patterns.add(index, null);
                this.regexps.add(index, null);
                this.regexpMessages.add(index, null);
                this.tooltips.add(index, null);
                this.valueTooltips.add(index, null);

                List<NativeHashMap<GGroupObjectValue, PValue>> list = new ArrayList<>();
                for (int i = 0; i < property.lastReaders.size(); i++)
                    list.add(null);
                lastAggrs.add(index, list);
            }
            this.columnKeys.set(index, columnKeys);
        } else
            assert index >= 0;

        NativeHashMap<GGroupObjectValue, PValue> valuesMap = this.values.get(index);
        if (updateKeys && valuesMap != null) {
            valuesMap.putAll(values);
        } else {
            NativeHashMap<GGroupObjectValue, PValue> pvalues = new NativeHashMap<>();
            pvalues.putAll(values);
            this.values.set(index, pvalues);
        }

        dataUpdated = true;
    }

    @Override
    public void updatePropertyCaptions(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.captions.set(properties.indexOf(property), values);

        dataUpdated = true;
    }

    @Override
    public void updateCaptionElementClasses(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.captionElementClasses.set(properties.indexOf(property), values);

        dataUpdated = true;
    }

    @Override
    public void updateLoadings(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updatePropertyFooters(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateLastValues(GPropertyDraw property, int index, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.lastAggrs.get(properties.indexOf(property)).set(index, values);

        dataUpdated = true;
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        int index = properties.indexOf(property);
        properties.remove(index);
        columnKeys.remove(index);
        captions.remove(index);
        lastAggrs.remove(index);
        values.remove(index);
        readOnlys.remove(index);
        showIfs.remove(index);

        dataUpdated = true;
    }

    private boolean rerender;
    protected void rerender() {
        rerender = true;
    }
    private boolean updateState;

    public void updateView(boolean dataUpdated, Boolean updateState) {
        if(updateState != null)
            this.updateState = updateState;

        if(dataUpdated || rerender) {
            updateView();
            updatePageSizeState(isPageSizeHit());
            rerender = false;
        }

        updateRendererState(this.updateState); // update state with server response
    }

    protected abstract void updateView();
    protected void updatePageSizeState(boolean hit) {
        messageLabel.setText(ClientMessages.Instance.get().formGridPageSizeHit(keys == null ? getPageSize() : keys.size() )); //need to show current objects size
        getPageSizeWidget().setVisible(hit);
    }
    protected abstract Element getRendererAreaElement();

    private long lastRendererDropped = 0;
    protected void updateRendererState(boolean set) {
        Runnable setFilter = () -> setOpacity(set, getRendererAreaElement());

        if(set) {
            long wasRendererDropped = lastRendererDropped;
            Scheduler.get().scheduleFixedDelay(() -> {
                if(wasRendererDropped == lastRendererDropped) // since set and drop has different timeouts
                    setFilter.run();
                return false;
            }, (int) MainFrame.updateRendererStateSetTimeout);
        } else {
            lastRendererDropped++;
            setFilter.run();
        }
    }

    @Override
    public void update(Boolean updateState) {
        updateView(dataUpdated, updateState);

        dataUpdated = false;
    }

    @Override
    public boolean isNoColumns() {
        return properties.isEmpty();
    }

    @Override
    public long getSetRequestIndex() {
        return setRequestIndex;
    }

    @Override
    public void setSetRequestIndex(long index) {
        setRequestIndex = index;
    }

    // ignore for now
    @Override
    public void focusProperty(GPropertyDraw propertyDraw) {
        focus(FocusUtils.Reason.ACTIVATE);
    }

    @Override
    public boolean changePropertyOrders(LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet) {
        return false;
    }

    @Override
    public void changePropertyOrders(LinkedHashMap<GPropertyDraw, GOrder> value) {
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        rowBackgroundValues = values;
    }
    
    public String getRowBackgroundColor(GGroupObjectValue key) {
        return PValue.getColorStringValue(rowBackgroundValues.get(key));
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        rowForegroundValues = values;
    }

    public String getRowForegroundColor(GGroupObjectValue key) {
        return PValue.getColorStringValue(rowForegroundValues.get(key));
    }

    @Override
    public void updateCellGridElementClasses(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.cellGridElementClasses.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateCellValueElementClasses(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.cellValueElementClasses.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateCellFontValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.cellFontValues.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.cellBackgroundValues.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.cellForegroundValues.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updatePlaceholderValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.placeholders.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updatePatternValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.patterns.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateRegexpValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.regexps.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateRegexpMessageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.regexpMessages.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateTooltipValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.tooltips.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateValueTooltipValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.valueTooltips.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateImageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {

    }

    @Override
    public void updateShowIfValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.showIfs.set(properties.indexOf(property), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateReadOnlyValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        this.readOnlys.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public GGroupObjectValue getSelectedKey() {
        return currentKey; // for executing actions used for wysiwyg
    }

    protected boolean isCurrentKey(GGroupObjectValue object){
        return Objects.equals(object, getSelectedKey());
    }

    @Override
    public GPropertyDraw getCurrentProperty() {
        if(!properties.isEmpty())
            return properties.get(0);
        return null;
    }

    @Override
    public GGroupObjectValue getCurrentColumnKey() {
        if(!properties.isEmpty())
            return columnKeys.get(0).get(0);
        return null;
    }

    @Override
    public int getSelectedRow() {
        return -1;
    }

    @Override
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {

    }

    @Override
    public void runGroupReport() {
    }

    @Override
    public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return null;
    }

    @Override
    public List<Pair<Column, String>> getFilterColumns() {
        List<Pair<Column, String>> result = new ArrayList<>();
        for(int i=0,size=properties.size();i<size;i++) {
            GPropertyDraw property = properties.get(i);
            NativeHashMap<GGroupObjectValue, PValue> propertyCaptions = captions.get(i);
            List<GGroupObjectValue> columns = columnKeys.get(i);
            for (GGroupObjectValue column : columns)
                result.add(GGridPropertyTable.getFilterColumn(property, column, GGridPropertyTable.getPropertyCaption(propertyCaptions, property, column)));
        }
        return result;
    }

    protected static String getColumnSID(GPropertyDraw property, int c, GGroupObjectValue columnKey) {
        return property.integrationSID + (columnKey.isEmpty() ? "" : "_" + c);
    }

    @Override
    public boolean hasUserPreferences() {
        return false;
    }

    @Override
    public boolean containsProperty(GPropertyDraw property) {
        return properties.indexOf(property) >= 0;
    }

    @Override
    public LinkedHashMap<GPropertyDraw, Boolean> getUserOrders(List<GPropertyDraw> propertyDrawList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GGroupObjectUserPreferences getCurrentUserGridPreferences() {
        return null;
    }

    @Override
    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return null;
    }

    protected long changeGroupObject(GGroupObjectValue value, boolean rendered) {
        setCurrentKey(value, rendered);
        return form.changeGroupObject(grid.groupObject, value);
    }

    public PValue getValue(GPropertyDraw property, GGroupObjectValue fullKey) {
        return values.get(properties.indexOf(property)).get(fullKey);
    }

    protected boolean isReadOnly(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey, boolean rendered) {
        if(property.isReadOnly() != null)
            return true;

        // when result is rendered, it's important to have property pending change mechanism to cancel changes when server ignores this changes
        if(rendered && !property.hasExternalChangeActionForRendering(RendererType.SIMPLE))
            return true;

        NativeHashMap<GGroupObjectValue, PValue> readOnlyValues = readOnlys.get(properties.indexOf(property));
        if(readOnlyValues == null)
            return false;

        return PValue.getBooleanValue(readOnlyValues.get(GGroupObjectValue.getFullKey(rowKey, columnKey)));
    }

    protected String getCellGridElementClass(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> cellGridElementClass = cellGridElementClasses.get(properties.indexOf(property));
        if(cellGridElementClass == null)
            return null;

        return PValue.getClassStringValue(cellGridElementClass.get(GGroupObjectValue.getFullKey(rowKey, columnKey)));
    }

    protected String getCellValueElementClass(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> cellValueElementClass = cellValueElementClasses.get(properties.indexOf(property));
        if(cellValueElementClass == null)
            return null;

        return PValue.getClassStringValue(cellValueElementClass.get(GGroupObjectValue.getFullKey(rowKey, columnKey)));
    }

    protected String getCaptionElementClass(GPropertyDraw property, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> cellCaptionElementClass = captionElementClasses.get(properties.indexOf(property));
        if(cellCaptionElementClass == null)
            return null;

        return PValue.getClassStringValue(cellCaptionElementClass.get(columnKey));
    }

    protected PValue getCellFont(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> cellFont = cellFontValues.get(properties.indexOf(property));
        if(cellFont == null)
            return null;

        return cellFont.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected PValue getCellBackground(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> cellBackground = cellBackgroundValues.get(properties.indexOf(property));
        if(cellBackground == null)
            return null;

        return cellBackground.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected PValue getCellPlaceholder(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> placeholder = placeholders.get(properties.indexOf(property));
        if(placeholder == null)
            return null;

        return placeholder.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected PValue getCellPattern(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> pattern = patterns.get(properties.indexOf(property));
        if(pattern == null)
            return null;

        return pattern.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected PValue getCellRegexp(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> regexp = regexps.get(properties.indexOf(property));
        if(regexp == null)
            return null;

        return regexp.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected PValue getCellRegexpMessage(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> regexpMessage = regexpMessages.get(properties.indexOf(property));
        if(regexpMessage == null)
            return null;

        return regexpMessage.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected PValue getCellTooltip(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> tooltip = tooltips.get(properties.indexOf(property));
        if(tooltip == null)
            return null;

        return tooltip.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected PValue getCellValueTooltip(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> valueTooltip = valueTooltips.get(properties.indexOf(property));
        if(valueTooltip == null)
            return null;

        return valueTooltip.get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected String getGridElementClass(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        return getCellGridElementClass(property, rowKey, columnKey);
    }

    protected String getValueElementClass(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        return getCellValueElementClass(property, rowKey, columnKey);
    }

    protected GFont getFont(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        PValue cellFont = getCellFont(property, rowKey, columnKey);
        return cellFont == null ? property.font : PValue.getFontValue(cellFont);
    }

    protected String getBackground(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        PValue cellBackground = getCellBackground(property, rowKey, columnKey);
        return cellBackground == null ? property.getBackground() : PValue.getColorStringValue(cellBackground);
    }

    protected String getPlaceholder(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        PValue placeholder = getCellPlaceholder(property, rowKey, columnKey);
        return placeholder == null ? property.placeholder : PValue.getStringValue(placeholder);
    }

    protected String getPattern(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        PValue pattern = getCellPattern(property, rowKey, columnKey);
        return pattern == null ? property.getPattern() : PValue.getStringValue(pattern);
    }

    protected String getRegexp(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        PValue regexp = getCellRegexp(property, rowKey, columnKey);
        return regexp == null ? property.regexp : PValue.getStringValue(regexp);
    }

    protected String getRegexpMessage(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        PValue regexpMessage = getCellRegexpMessage(property, rowKey, columnKey);
        return regexpMessage == null ? property.regexpMessage : PValue.getStringValue(regexpMessage);
    }

    protected String getTooltip(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        PValue tooltip = getCellTooltip(property, rowKey, columnKey);
        return tooltip == null ? property.tooltip : PValue.getStringValue(tooltip);
    }

    protected String getValueTooltip(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        PValue valueTooltip = getCellValueTooltip(property, rowKey, columnKey);
        return valueTooltip == null ? property.valueTooltip : PValue.getStringValue(valueTooltip);
    }

    protected String getCellForeground(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, PValue> cellForeground = cellForegroundValues.get(properties.indexOf(property));
        if(cellForeground == null)
            return null;

        return PValue.getColorStringValue(cellForeground.get(GGroupObjectValue.getFullKey(rowKey, columnKey)));
    }

    protected String getForeground(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        String cellForeground = getCellForeground(property, rowKey, columnKey);
        return cellForeground == null ? property.getForeground() : cellForeground;
    }
    // utils

    protected JsArray<JavaScriptObject> convertToObjectsString(JsArray<JsArrayString> array) {
        JsArrayString columns = array.get(0);
        JsArray<JavaScriptObject> convert = JavaScriptObject.createArray().cast();
        for(int i=1;i<array.length();i++) {
            WrapperObject object = JavaScriptObject.createObject().cast();
            JsArrayString values = array.get(i);
            for(int j=0;j<columns.length();j++) {
                object.putValue(columns.get(j), fromString(values.get(j)));
            }
            convert.push(object);
        }
        return convert;
    }

    protected JsArray<JavaScriptObject> convertToObjectsMixed(JsArray<JsArray<JavaScriptObject>> array) {
        JsArray<JavaScriptObject> columns = array.get(0); // actually strings
        JsArray<JavaScriptObject> convert = JavaScriptObject.createArray().cast();
        for(int i=1;i<array.length();i++) {
            WrapperObject object = JavaScriptObject.createObject().cast();
            JsArray<JavaScriptObject> values = array.get(i);
            for(int j=0;j<columns.length();j++) {
                object.putValue(toString(columns.get(j)), values.get(j));
            }
            convert.push(object);
        }
        return convert;
    }

    protected static void setOpacity(boolean updateState, Element element) {
        if (updateState) {
            element.getStyle().setProperty("filter", "opacity(0.5)");
        } else {
            //there is a bug with position:fixed and opacity parameter
            element.getStyle().setProperty("filter", "");
        }
    }

    static class WrapperObject extends JavaScriptObject {
        protected WrapperObject() {
        }

        protected native final JsArrayString getKeys() /*-{
            return Object.keys(this);
        }-*/;
        protected native final JsArrayString getArrayString(String string) /*-{
            return this[string];
        }-*/;
        protected native final JsArrayInteger getArrayInteger(String string) /*-{
            return this[string];
        }-*/;
        protected native final JsArrayMixed getArrayMixed(String string) /*-{
            return this[string];
        }-*/;
        protected native final void putValue(String key, JavaScriptObject object) /*-{
            this[key] = object;
        }-*/;
        protected native final JavaScriptObject getValue(String key) /*-{
            return this[key];
        }-*/;
    }

    protected static native boolean hasKey(JavaScriptObject object, String key) /*-{
        return object[key] !== undefined;
    }-*/;
    protected static native JavaScriptObject getValue(JavaScriptObject object, String key) /*-{
        return object[key];
    }-*/;

    public static native JavaScriptObject fromString(String string) /*-{
        return string;
    }-*/;
    public static native String toString(JavaScriptObject string) /*-{
        return string;
    }-*/;
    protected static native JavaScriptObject fromDouble(double d) /*-{
        return d;
    }-*/;
    public static native int toInt(JavaScriptObject d) /*-{
        return d;
    }-*/;
    protected static native double toDouble(JavaScriptObject d) /*-{
        return d;
    }-*/;
    protected static native JavaScriptObject fromBoolean(boolean b) /*-{
        return b;
    }-*/;
    protected static native boolean toBoolean(JavaScriptObject b) /*-{
        return b;
    }-*/;
    public static native <T> JavaScriptObject fromObject(T object) /*-{
        return object;
    }-*/;
    public static native <T> T toObject(JavaScriptObject object) /*-{
        return object;
    }-*/;
}
