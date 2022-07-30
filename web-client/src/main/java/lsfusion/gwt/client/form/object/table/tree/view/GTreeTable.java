package lsfusion.gwt.client.form.object.table.tree.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.JSNIHelper;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GMouseInputEvent;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableFooter;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GTreeTable extends GGridPropertyTable<GTreeGridRecord> {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    
    private boolean dataUpdated;

    private GTreeTableTree tree;

    private TreeTableSelectionHandler treeSelectionHandler;

    private GTreeGroupController treeGroupController;

    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> values = new NativeSIDMap<>();
    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> loadings = new NativeSIDMap<>();
    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> readOnly = new NativeSIDMap<>();

    private GTreeGroup treeGroup;

    @Override
    public void focus(FocusUtils.Reason reason) {
        FocusUtils.focus(getTableDataFocusElement(), reason);
    }

    private GSize hierarchicalWidth;

    public GTreeTable(GFormController iformController, GForm iform, GTreeGroupController treeGroupController, GTreeGroup treeGroup, TableContainer tableContainer) {
        super(iformController, lastGroupObject(treeGroup), tableContainer, treeGroupController.getFont());

        this.treeGroupController = treeGroupController;
        this.treeGroup = treeGroup;

        tree = new GTreeTableTree(iform);

        Column<GTreeGridRecord, Object> column = new ExpandTreeColumn();
        GGridPropertyTableHeader header = noHeaders ? null : new GGridPropertyTableHeader(this, messages.formTree(), null, false);
        addColumn(column, header, null);

        hierarchicalWidth = treeGroup.getExpandWidth();

        treeSelectionHandler = new TreeTableSelectionHandler(this);
        setSelectionHandler(treeSelectionHandler);

        setRowChangedHandler(() -> {
            final GTreeGridRecord kbSelectedRecord = getSelectedRowValue();
            if (kbSelectedRecord != null)
                form.changeGroupObjectLater(kbSelectedRecord.getGroup(), kbSelectedRecord.getKey());
        });

        sortableHeaderManager = new GGridSortableHeaderManager<GPropertyDraw>(this, true) {
            @Override
            protected void orderChanged(GPropertyDraw columnKey, GOrder modiType) {
                form.changePropertyOrder(columnKey, GGroupObjectValue.EMPTY, modiType);
            }

            @Override
            protected void ordersSet(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders) {
                List<Integer> propertyList = new ArrayList<>();
                List<GGroupObjectValue> columnKeyList = new ArrayList<>();
                List<Boolean> orderList = new ArrayList<>();
                for(Map.Entry<GPropertyDraw, Boolean> entry : orders.entrySet()) {
                    propertyList.add(entry.getKey().ID);
                    columnKeyList.add(GGroupObjectValue.EMPTY);
                    orderList.add(entry.getValue());
                }

                form.setPropertyOrders(groupObject, propertyList, columnKeyList, orderList);
            }

            @Override
            protected GPropertyDraw getColumnKey(int column) {
                return getTreeGridColumn(column).getColumnProperty();
            }
        };

        if(treeGroupController.isExpandOnClick())
            form.addBinding(new GMouseInputEvent(GMouseInputEvent.DBLCLK)::isEvent, new GBindingEnv(100, GBindingMode.ONLY, null, GBindingMode.ONLY, null, null, null, null),
                    () -> {
                        GTreeObjectTableNode node = getExpandSelectedNode();
                        return node != null && node.isExpandable();
                    },
                    event -> {
                        fireExpandSelectedNode(null);
                    }, getWidget(), groupObject);
    }

    private static GGroupObject lastGroupObject(GTreeGroup treeGroup) {
        return treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;
    }

    public void removeProperty(GPropertyDraw property) {
        dataUpdated = true;

        values.remove(property);
        loadings.remove(property);
        int index = tree.removeProperty(property);
        if(index > -1) // we need only last group, just like everywhere
            removeColumn(index);

        columnsUpdated = true;
    }

    public boolean isPropertyShown(GPropertyDraw property) {
        return tree.getPropertyIndex(property) != -1;
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
       focusColumn(tree.getPropertyIndex(propertyDraw), FocusUtils.Reason.ACTIVATE);
    }

    public void updateProperty(GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        dataUpdated = true;

        if(!updateKeys) {
            int index = tree.updateProperty(property);

            if (index > -1) {
                GridColumn gridColumn = new GridColumn(property);
                String propertyCaption = getPropertyCaption(property);
                GGridPropertyTableHeader header = noHeaders ? null : new GGridPropertyTableHeader(this, propertyCaption, property.getTooltipText(propertyCaption), gridColumn.isSticky());
                GGridPropertyTableFooter footer = noFooters ? null : new GGridPropertyTableFooter(this, property, null, null, gridColumn.isSticky());

                insertColumn(index, gridColumn, header, footer);

                columnsUpdated = true;
            }
        }
        updatePropertyValues(property, values, updateKeys);
    }

    private interface TreeGridColumn  {
        GPropertyDraw getColumnProperty();
    }

    private final static String ICON_LEAF = "tree_leaf.png";
    private final static String ICON_OPEN = "tree_open.png";
    private final static String ICON_CLOSED = "tree_closed.png";
    private final static String ICON_PASSBY = "tree_dots_passby.png";
    private final static String ICON_EMPTY = "tree_empty.png";
    private final static String ICON_BRANCH = "tree_dots_branch.png";

    private final static String TREE_NODE_ATTRIBUTE = "__tree_node";

    public static void renderExpandDom(Element cellElement, GTreeColumnValue treeValue) {
        GPropertyTableBuilder.setRowHeight(cellElement, GSize.ZERO, false); // somewhy it's needed for proper indent showing
        for (int i = 0; i <= treeValue.level; i++) {
            DivElement img = createIndentElement(cellElement);
            updateIndentElement(img, treeValue, i);
        }
    }

    private static DivElement createIndentElement(Element cellElement) {
        DivElement div = cellElement.appendChild(Document.get().createDivElement());
        div.getStyle().setFloat(Style.Float.LEFT);
        div.getStyle().setHeight(100, Style.Unit.PCT);
        div.getStyle().setWidth(16, Style.Unit.PX);

        DivElement vert = Document.get().createDivElement();
        vert.getStyle().setWidth(16, Style.Unit.PX);
        vert.getStyle().setHeight(100, Style.Unit.PCT);

        DivElement top = vert.appendChild(Document.get().createDivElement());
        top.getStyle().setHeight(50, Style.Unit.PCT);

        DivElement bottom = vert.appendChild(Document.get().createDivElement());
        bottom.getStyle().setHeight(50, Style.Unit.PCT);
        bottom.getStyle().setPosition(Style.Position.RELATIVE);

        ImageElement img = bottom.appendChild(Document.get().createImageElement());
        img.getStyle().setPosition(Style.Position.ABSOLUTE);
        img.getStyle().setTop(-8, Style.Unit.PX);

        return div.appendChild(vert);
    }

    private static void updateIndentElement(DivElement element, GTreeColumnValue treeValue, int indentLevel) {
        String indentIcon;
        ImageElement img = element.getElementsByTagName("img").getItem(0).cast();
        int nodeLevel = treeValue.level;
        if (indentLevel < nodeLevel - 1) {
            indentIcon = treeValue.lastInLevelMap[indentLevel] ? ICON_EMPTY : ICON_PASSBY;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else if (indentLevel == nodeLevel - 1) {
            indentIcon = ICON_BRANCH;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else {
            assert indentLevel == nodeLevel;
            img.setAttribute(TREE_NODE_ATTRIBUTE, "true");
            indentIcon = getNodeIcon(treeValue);
        }

        if (ICON_PASSBY.equals(indentIcon)) {
            changeDots(element, true, true);
        } else if (ICON_BRANCH.equals(indentIcon)) {
            if (treeValue.lastInLevelMap[indentLevel]) {
                changeDots(element, true, false); //end
            } else {
                changeDots(element, true, true); //branch
            }
        } else if (ICON_EMPTY.equals(indentIcon) || ICON_LEAF.equals(indentIcon)) {
            changeDots(element, false, false);
        } else if (ICON_CLOSED.equals(indentIcon)) {
            changeDots(element, false, treeValue.closedDotBottom);
        }else if (ICON_OPEN.equals(indentIcon)) {
            changeDots(element, false, treeValue.openDotBottom);
        }

        if(ICON_CLOSED.equals(indentIcon)) {
            img.removeClassName("expanded-image");
            img.addClassName("collapsed-image");
        } else if(ICON_OPEN.equals(indentIcon)) {
            img.removeClassName("collapsed-image");
            img.addClassName("expanded-image");
        } else if(ICON_LEAF.equals(indentIcon)) {
            img.addClassName("leaf-image");
        } else if(ICON_BRANCH.equals(indentIcon)) {
            img.addClassName("branch-image");
        }

        GwtClientUtils.setThemeImage(ICON_PASSBY.equals(indentIcon) ? ICON_EMPTY : indentIcon, img::setSrc);
    }

    private static void changeDots(DivElement element, boolean dotTop, boolean dotBottom) {
        Element top = element.getFirstChild().cast();
        Element bottom = element.getLastChild().cast();

        if (dotTop && dotBottom) {
            ensureDotsAndSetBackground(element);
            element.getStyle().setProperty("backgroundRepeat", "no-repeat repeat");
            clearBackground(top);
            clearBackground(bottom);
            return;
        } else {
            clearBackground(element);
        }
        if (dotTop) {
            ensureDotsAndSetBackground(top);
            top.getStyle().setProperty("backgroundRepeat", "no-repeat repeat");
        } else {
            clearBackground(top);
        }

        if (dotBottom) {
            ensureDotsAndSetBackground(bottom);
            bottom.getStyle().setProperty("backgroundRepeat", "no-repeat repeat");
        } else {
            clearBackground(bottom);
        }
    }

    private static void ensureDotsAndSetBackground(Element element) {
        element.addClassName("passby-image");
        GwtClientUtils.setThemeImage(ICON_PASSBY, str -> element.getStyle().setBackgroundImage("url('" + str + "')"));
    }

    private static void clearBackground(Element element) {
        element.removeClassName("passby-image");
        element.getStyle().clearBackgroundImage();
    }

    private static String getNodeIcon(GTreeColumnValue treeValue) {
        switch (treeValue.type) {
            case LEAF:
                return ICON_LEAF;
            case OPEN:
                return ICON_OPEN;
            case CLOSED:
                return ICON_CLOSED;
            case LOADING:
                return CellRenderer.ICON_LOADING;
        }
        throw new UnsupportedOperationException();
    }

    private static class RenderedState {
        public GTreeColumnValue value;

        public String foreground;
        public String background;
    }

    // actually singleton
    private class ExpandTreeColumn extends Column<GTreeGridRecord, Object> implements TreeGridColumn {

        @Override
        public String getNativeSID() {
            return "_EXPAND_COLUMN";
        }

        private GTreeColumnValue getTreeValue(Cell cell) {
            return getTreeGridRow(cell).getTreeValue();
        }

        @Override
        public boolean isFocusable() {
            return true;
        }

        @Override
        public boolean isSticky() {
            return false;
        }

        @Override
        public GPropertyDraw getColumnProperty() {
            return null;
        }

        @Override
        public void onEditEvent(EventHandler handler, Cell editCell, Element editRenderElement) {
            Event event = handler.event;
            boolean changeEvent = GMouseStroke.isChangeEvent(event);
            if (changeEvent || (treeGroupController.isExpandOnClick() && GMouseStroke.isDoubleChangeEvent(event))) { // we need to consume double click event to prevent treetable global dblclick binding (in this case node will be collapsed / expanded once again)
                String attrID = JSNIHelper.getAttributeOrNull(Element.as(event.getEventTarget()), TREE_NODE_ATTRIBUTE);
                if (attrID != null) {
                    boolean consumed = false;
                    if(changeEvent)
                        consumed = changeTreeState(editCell);
                    if(consumed)
                        handler.consume();
                }
            }
        }

        private boolean changeTreeState(Cell cell) {
            return fireExpandNode(tree.getExpandNodeByRecord(getTreeGridRow(cell)), null);
        }

        @Override
        public void renderDom(Cell cell, TableCellElement cellElement) {
            if(cell.getRow() != null) // can be cell in column row
                GTreeTable.renderExpandDom(cellElement, getTreeValue(cell));
        }

        private static final String RENDERED = "renderedTree";

        @Override
        public void updateDom(Cell cell, TableCellElement cellElement) {

            RenderedState renderedState = (RenderedState) cellElement.getPropertyObject(RENDERED);
            boolean isNew = false;
            if(renderedState == null) {
                renderedState = new RenderedState();
                cellElement.setPropertyObject(RENDERED, renderedState);

                isNew = true;
            }

            GTreeGridRecord rowValue = (GTreeGridRecord) cell.getRow();
            String background = DataGrid.getSelectedCellBackground(isSelectedRow(cell), isFocusedColumn(cell), rowValue.getRowBackground());
            String foreground = rowValue.getRowForeground();
            if(isNew || !equalsColorState(renderedState, background, foreground)) {
                renderedState.background = background;
                renderedState.foreground = foreground;

                AbstractDataGridBuilder.updateColors(cellElement, background, foreground);
            }

            GTreeColumnValue treeValue = getTreeValue(cell);
            if(isNew || !equalsDynamicState(renderedState, treeValue)) {
                renderedState.value = treeValue;

                renderDynamicContent(cellElement, treeValue);
            }
        }

        private boolean equalsDynamicState(RenderedState state, GTreeColumnValue value) {
            return state.value.equalsValue(value);
        }
        private boolean equalsColorState(RenderedState state, String background, String foreground) {
            return GwtClientUtils.nullEquals(state.background, background) && GwtClientUtils.nullEquals(state.foreground, foreground);
        }

        private void renderDynamicContent(TableCellElement cellElement, GTreeColumnValue treeValue) {
            while (cellElement.getChildCount() > treeValue.level + 1) {
                cellElement.getLastChild().removeFromParent();
            }

            for (int i = 0; i <= treeValue.level; i++) {
                DivElement img;
                if (i >= cellElement.getChildCount()) {
                    img = createIndentElement(cellElement);
                } else {
                    img = cellElement.getChild(i).getFirstChild().cast();
                }

                updateIndentElement(img, treeValue, i);
            }
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }
    }

    private class GridColumn extends GridPropertyColumn implements TreeGridColumn {

        private final GPropertyDraw columnProperty;

        @Override
        public String getNativeSID() {
            return columnProperty.getNativeSID();
        }

        @Override
        public boolean isFocusable() {
            return GTreeTable.this.isFocusable(columnProperty);
        }

        @Override
        public boolean isSticky() {
            return columnProperty.sticky;
        }

        public GridColumn(GPropertyDraw columnProperty) {
            this.columnProperty = columnProperty;
        }

        @Override
        protected Object getValue(GPropertyDraw property, GTreeGridRecord record) {
            return record.getValue(property);
        }

        @Override
        protected boolean isLoading(GPropertyDraw property, GTreeGridRecord record) {
            return record.isLoading(property);
        }

        @Override
        protected Object getImage(GPropertyDraw property, GTreeGridRecord record) {
            return record.getImage(property);
        }

        @Override
        protected String getBackground(GPropertyDraw property, GTreeGridRecord record) {
            return record.getBackground(property);
        }

        @Override
        protected String getForeground(GPropertyDraw property, GTreeGridRecord record) {
            return record.getForeground(property);
        }

        // in tree property might change
        private static final String PDRAW_ATTRIBUTE = "__gwt_pdraw"; // actually it represents nod depth

        @Override
        public void renderDom(Cell cell, TableCellElement cellElement) {
            super.renderDom(cell, cellElement);

            cellElement.setPropertyObject(PDRAW_ATTRIBUTE, getProperty(cell));
        }

        @Override
        public void updateDom(Cell cell, TableCellElement cellElement) {
            GPropertyDraw newProperty = getProperty(cell);
            GPropertyDraw oldProperty = ((GPropertyDraw)cellElement.getPropertyObject(PDRAW_ATTRIBUTE));
            // if property changed - rerender
            if(!GwtClientUtils.nullEquals(oldProperty, newProperty) && !form.isEditing()) { // we don't want to clear editing (it will be rerendered anyway, however not sure if this check is needed)
                if(oldProperty != null) {
                    if(!GPropertyTableBuilder.clearRenderSized(cellElement, oldProperty)) {
                        assert cellElement == GPropertyTableBuilder.getRenderSizedElement(cellElement, oldProperty);
                        oldProperty.getCellRenderer().clearRender(cellElement, getRenderContext(cell, cellElement, oldProperty, this));
                    }
                    cellElement.setPropertyObject(PDRAW_ATTRIBUTE, null);
                }

                renderDom(cell, cellElement);
            }

            super.updateDom(cell, cellElement);
        }

        @Override
        public GPropertyDraw getColumnProperty() {
            return columnProperty;
        }

        @Override
        public boolean isCustomRenderer() {
            return columnProperty.getCellRenderer().isCustomRenderer();
        }
    }

    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents, NativeHashMap<GGroupObjectValue, Integer> expandable, int requestIndex) {
        tree.setKeys(group, keys, parents, expandable, requestIndex);

        dataUpdated = true;
    }

    public void updateLoadings(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> propLoadings) {
        // here can be leaks because of nulls (so in theory nulls better to be removed)
        GwtSharedUtils.putUpdate(loadings, property, propLoadings, true);

        dataUpdated = true;
    }

    public void updatePropertyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        GwtSharedUtils.putUpdate(values, property, propValues, updateKeys);

        dataUpdated = true;
    }

    public void updateReadOnlyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> readOnlyValues) {
        GwtSharedUtils.putUpdate(readOnly, property, readOnlyValues, false);

        dataUpdated = true;
    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        super.updateCellBackgroundValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        super.updateCellForegroundValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateCellImages(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        super.updateCellImages(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        super.updateRowBackgroundValues(values);
        dataUpdated = true;
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        super.updateRowForegroundValues(values);
        dataUpdated = true;
    }

    private Integer hierarchicalUserWidth = null;
    private Double hierarchicalUserFlex = null;
    private final NativeSIDMap<GPropertyDraw, Integer> userWidths = new NativeSIDMap<>();
    private final NativeSIDMap<GPropertyDraw, Double> userFlexes = new NativeSIDMap<>();
    @Override
    protected void setUserWidth(GPropertyDraw property, Integer value) {
        userWidths.put(property, value);
    }

    @Override
    protected void setUserFlex(GPropertyDraw property, Double value) {
        userFlexes.put(property, value);
    }

    @Override
    protected Integer getUserWidth(GPropertyDraw property) {
        return userWidths.get(property);
    }

    @Override
    protected Double getUserFlex(GPropertyDraw property) {
        return userFlexes.get(property);
    }

    @Override
    protected void setUserWidth(int i, int width) {
        if(i==0) {
            hierarchicalUserWidth = width;
            return;
        }
        super.setUserWidth(i, width);
    }

    @Override
    protected Integer getUserWidth(int i) {
        if(i==0)
            return hierarchicalUserWidth;
        return super.getUserWidth(i);
    }

    @Override
    protected void setUserFlex(int i, double flex) {
        if(i==0) {
            hierarchicalUserFlex = flex;
            return;
        }
        super.setUserFlex(i, flex);
    }

    @Override
    protected Double getUserFlex(int i) {
        if(i==0)
            return hierarchicalUserFlex;
        return super.getUserFlex(i);
    }

    public GTreeGridRecord getTreeGridRow(Cell editCell) {
        return (GTreeGridRecord) editCell.getRow();
    }
    protected TreeGridColumn getTreeGridColumn(int i) {
        return (TreeGridColumn) getColumn(i);
    }
    public GridColumn getGridColumn(int i) {
        assert i > 0;
        return (GridColumn)super.getGridColumn(i);
    }

    @Override
    protected GPropertyDraw getColumnPropertyDraw(int i) {
        return getGridColumn(i).getColumnProperty();
    }

    @Override
    protected GGroupObjectValue getColumnKey(int i) {
        return GGroupObjectValue.EMPTY;
    }

    @Override
    protected void updatePropertyHeader(int index) {
        if(index > 0) {
            super.updatePropertyHeader(index);
        }
    }

    @Override
    public void updatePropertyFooter(int index) {
        if(index > 0) {
            super.updatePropertyFooter(index);
        }
    }

    @Override
    public GSize getHeaderHeight() {
        return treeGroup.getHeaderHeight();
    }

    @Override
    protected double getColumnFlex(int i) {
        if(i == 0)
            return hierarchicalWidth.getValueFlexSize();
        return super.getColumnFlex(i);
    }

    @Override
    protected GSize getColumnBaseWidth(int i) {
        if(i==0)
            return hierarchicalWidth;
        return super.getColumnBaseWidth(i);
    }

    public void update() {
        updateColumns();

        updateCaptions();
        updateFooters();

        updateData();
    }

    public void updateData() {
        if (dataUpdated) {
            checkUpdateCurrentRow();

//            checkSelectedRowVisible();

            rows.clear();
            tree.updateRows(rows);
            updatePropertyReaders();
//            treeSelectionHandler.dataUpdated();

            rowsChanged();

            dataUpdated = false;
        }

        updateCurrentRow();
    }

    public void updateColumns() {
        if(columnsUpdated) {
            for (int i = 1, size = getColumnCount(); i < size; i++) {
                updatePropertyHeader(i);
                updatePropertyFooter(i);
            }

            updateLayoutWidth();

            columnsChanged();

            columnsUpdated = false;
            captionsUpdated = false;
            footersUpdated = false;
        }
    }

    @Override
    public void onResize() {
        if (getWidget().isVisible()) {
            super.onResize();
        }
    }

    protected void updatePropertyReaders() {
        for (GTreeGridRecord record : rows) {
            GGroupObjectValue key = record.getKey();

            record.setRowBackground(rowBackgroundValues.get(key));
            record.setRowForeground(rowForegroundValues.get(key));

            if(record instanceof GTreeObjectGridRecord) {
                GTreeObjectGridRecord objectRecord = (GTreeObjectGridRecord)record;

                for (int i = 1, size = getColumnCount(); i < size; i++) {
                    GPropertyDraw property = getProperty(objectRecord, i);
                    if (property != null) {
                        Object value = values.get(property).get(key);
                        objectRecord.setValue(property, value);

                        NativeHashMap<GGroupObjectValue, Object> loadingMap = loadings.get(property);
                        boolean loading = loadingMap != null && loadingMap.get(key) != null;
                        objectRecord.setLoading(property, loading);

                        Object background = null;
                        NativeHashMap<GGroupObjectValue, Object> propBackgrounds = cellBackgroundValues.get(property);
                        if (propBackgrounds != null)
                            background = propBackgrounds.get(key);
                        objectRecord.setBackground(property, background == null ? property.background : background);

                        Object foreground = null;
                        NativeHashMap<GGroupObjectValue, Object> propForegrounds = cellForegroundValues.get(property);
                        if (propForegrounds != null)
                            foreground = propForegrounds.get(key);
                        objectRecord.setForeground(property, foreground == null ? property.foreground : foreground);

                        if (property.hasDynamicImage()) {
                            NativeHashMap<GGroupObjectValue, Object> actionImages = cellImages.get(property);
                            objectRecord.setImage(property, actionImages == null ? null : actionImages.get(key));
                        }
                    }
                }
            }
        }
    }

    public void fireExpandNodeRecursive(boolean current, boolean open) {
        GTreeContainerTableNode node = getExpandSelectedNode();
        if (node != null && (!current || node.isExpandable())) {
            long requestIndex = form.expandGroupObjectRecursive(node.getGroup(), current, open);
            expandNodeRecursive(current ? node : tree.root, open, requestIndex);

            updateExpand();
        }
    }

    public void expandNodeRecursive(GTreeContainerTableNode node, boolean open, long requestIndex) {
        if(open) {
            for(GTreeChildTableNode child : node.getChildren())
                if(child instanceof GTreeObjectTableNode)
                    expandNodeRecursive((GTreeObjectTableNode)child, true, requestIndex);
            if(node.isExpandable() && !node.hasExpandableChildren())
                expandNode(node, true, requestIndex); // because it is expandable
        } else {
            if(node == tree.root) { // in that case we should collapse all root elements
                for (GTreeChildTableNode child : node.getChildren())
                    if (child instanceof GTreeObjectTableNode)
                        expandNode((GTreeObjectTableNode) child, false, requestIndex);
            } else
                expandNode(node, false, requestIndex);
        }
    }
    public void expandNode(GTreeContainerTableNode node, boolean open, long requestIndex) {
        node.setPendingExpanding(open, requestIndex);

        Pair<Integer, Integer> indexRange = null; GTreeObjectGridRecord record = null; int startFrom = 0, shift = 0;
        if(incrementalUpdate) {
            // finding row index and next sibling index
            indexRange = tree.incGetIndexRange(node);

            // changing expand column
            record = incUpdateExpandColumn(indexRange, open);
        }

        if(open) { // adding virtual "expandable" node
            assert !node.hasExpandableChildren();

            int addCount = node.getExpandableChildren();
            GTreeExpandingTableNode[] expandingNodes = new GTreeExpandingTableNode[addCount];
            for(int i=0;i<addCount;i++) {
                GTreeExpandingTableNode expandingNode = new GTreeExpandingTableNode(i);
                expandingNodes[i] = expandingNode;
                node.addNode(i, expandingNode);
            }
            if(incrementalUpdate) { // adding
                assert indexRange.first.equals(indexRange.second); // since this node should have no children
                int index = indexRange.first;

                // adding rows
                for(int i=0;i<addCount;i++) {
                    GTreeExpandingTableNode expandingNode = expandingNodes[i];
                    GTreeColumnValue treeValue = tree.createTreeColumnValue(expandingNode, node, record);
                    int addIndex = index + i;
                    GTreeExpandingGridRecord treeRecord = new GTreeExpandingGridRecord(addIndex, node, treeValue, expandingNode);
                    rows.add(addIndex, treeRecord);
                    tableBuilder.incBuildRow(tableWidget.getSection(), addIndex, treeRecord);
                }

                // shifting rows (here it's better to do to avoid dom reads)
                startFrom = index + addCount; shift = addCount;
            }
        } else { // removing all children nodes
            if(incrementalUpdate) {
                // removing all rows in that tange
                tableBuilder.incDeleteRows(tableWidget.getSection(), indexRange.first, indexRange.second);
                rows.removeRange(indexRange.first, indexRange.second);

                // shifting rows (here it's better to do to avoid dom reads)
                startFrom = indexRange.first; shift = indexRange.first - indexRange.second;
            }

            tree.removeChildrenFromGroupNodes(node);
            node.setChildren(new ArrayList<>());
        }

        if(incrementalUpdate)
            incUpdateRowIndices(startFrom, shift);
    }

    private GTreeObjectGridRecord incUpdateExpandColumn(Pair<Integer, Integer> indexRange, boolean open) {
        assert incrementalUpdate;

        int index = indexRange.first - 1;
        GTreeObjectGridRecord treeObjectRecord = (GTreeObjectGridRecord) getRowValue(index);
        treeObjectRecord.setTreeValue(treeObjectRecord.getTreeValue().override(GTreeColumnValueType.get(open)));
        tableBuilder.incUpdateRow(tableWidget.getSection(), index, new int[]{0}, treeObjectRecord);
        return treeObjectRecord;
    }

    private GTreeObjectTableNode getExpandSelectedNode() {
        return tree.getExpandNodeByRecord(getSelectedRowValue());
    }
    public boolean isCurrentPathExpanded() {
        GTreeObjectTableNode node;
        return (node = getExpandSelectedNode()) != null && node.isExpandable() && node.hasExpandableChildren();
    }

    public GGroupObject getGroupObject() {
        return null;
    }

    @Override
    public GAbstractTableController getGroupController() {
        return treeGroupController;
    }

    public GPropertyDraw getSelectedFilterProperty() {
        GPropertyDraw property = getSelectedProperty();
        if (property == null && getColumnCount() > 1) {
            property = getProperty(getSelectedCell(1));
        }
        return property;
    }

    public Object getSelectedValue(GPropertyDraw property) {
        GTreeGridRecord record = getSelectedRowValue();
        return record == null ? null : record.getValue(property);
    }

    public List<Pair<lsfusion.gwt.client.form.view.Column, String>> getSelectedColumns(GGroupObject selectedGroupObject) {
        ArrayList<GPropertyDraw> properties = tree.getProperties(selectedGroupObject);
        if (properties != null) {
            return properties.stream().map(property ->
                    getSelectedColumn(property, GGroupObjectValue.EMPTY)
            ).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public GPropertyDraw getProperty(Cell cell) {
        GTreeGridRecord rowValue = getTreeGridRow(cell);
        return rowValue == null ? null : getProperty(rowValue, cell.getColumnIndex());
    }

    public GPropertyDraw getProperty(GTreeGridRecord rowValue, int columnIndex) {
        return tree.getProperty(rowValue.getGroup(), columnIndex);
    }

    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return null;
    }
    @Override
    public GGroupObjectValue getColumnKey(Cell cell) {
        return GGroupObjectValue.EMPTY;
//        return currentRecords.get(context.getIndex()).getKey();
    }

    @Override
    public boolean isReadOnly(Cell cell) {
        GPropertyDraw property = getProperty(cell);
        if (property != null && !property.isReadOnly()) {
            NativeHashMap<GGroupObjectValue, Object> propReadOnly = readOnly.get(property);
            return propReadOnly != null && propReadOnly.get(getRowKey(cell)) != null;
        }
        return true;
    }

    @Override
    public GGroupObjectValue getRowKey(Cell editCell) {
        return getTreeGridRow(editCell).getKey();
    }

    @Override
    public void quickFilter(Event event, GPropertyDraw filterProperty, GGroupObjectValue columnKey) {
        treeGroupController.quickEditFilter(event, filterProperty, columnKey);
    }

    @Override
    public void setValueAt(Cell cell, Object value) {
        GPropertyDraw property = getProperty(cell);
        // assert property is not null since we want get here if property is null

        getTreeGridRow(cell).setValue(property, value);

        values.get(property).put(getRowKey(cell), value);
    }

    public Pair<GGroupObjectValue, Object> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, Object value) {
        return setLoadingValueAt(property, treeGroup.filterRowKeys(property.groupObject, fullCurrentKey), tree.getPropertyIndex(property), GGroupObjectValue.EMPTY, value);
    }

    @Override
    public void setLoadingAt(Cell cell) {
        GPropertyDraw property = getProperty(cell);
        // assert property is not null since we want get here if property is null

        getTreeGridRow(cell).setLoading(property, true);
        NativeHashMap<GGroupObjectValue, Object> loadingMap = loadings.get(property);
        if(loadingMap == null) {
            loadingMap = new NativeHashMap<>();
            loadings.put(property, loadingMap);
        }
        loadingMap.put(getRowKey(cell), true);
    }

    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        return sortableHeaderManager.changeOrders(groupObject, orders, alreadySet);
    }

    public boolean keyboardNodeChangeState(boolean open) {
        return fireExpandSelectedNode(open);
    }

    public boolean fireExpandSelectedNode(Boolean open) {
        return fireExpandNode(getExpandSelectedNode(), open);
    }

    // open = null - toggle
    public boolean fireExpandNode(GTreeObjectTableNode node, Boolean open) {
        if(node == null || !node.isExpandable())
            return false;

        boolean nodeIsOpen = node.hasExpandableChildren();
        if (open == null || !open.equals(nodeIsOpen)) {
            open = !nodeIsOpen;
            long requestIndex = form.expandGroupObject(node.getGroup(), node.getKey(), open);
            expandNode(node, open, requestIndex);
//                treeSelectionHandler.nodeTryingToExpand = node;

            updateExpand();

            return true;
        }
        return false;
    }

    public void updateExpand() {
        if (!incrementalUpdate) {
            dataUpdated = true;
            updateData();
        }
    }

    public class TreeTableSelectionHandler extends GridPropertyTableSelectionHandler<GTreeGridRecord> {
//        public GTreeTableNode nodeTryingToExpand = null;

        public TreeTableSelectionHandler(DataGrid<GTreeGridRecord> table) {
            super(table);
        }

//        public void dataUpdated() {
//            if (nodeTryingToExpand != null) {
//                if (!nodeTryingToExpand.isOpen()) {
//                    nextColumn(true);
//                }
//                nodeTryingToExpand = null;
//            }
//        }

        @Override
        public boolean handleKeyEvent(Event event) {
            assert BrowserEvents.KEYDOWN.equals(event.getType());

            int keyCode = event.getKeyCode();
            if (!event.getCtrlKey() && getSelectedColumn() == 0) {
                if (keyCode == KeyCodes.KEY_RIGHT) {
                    if (keyboardNodeChangeState(true)) {
                        return true;
                    }
                } else if (keyCode == KeyCodes.KEY_LEFT) {
                    if (keyboardNodeChangeState(false)) {
                        return true;
                    }
                }
            }

            return super.handleKeyEvent(event);
        }
    }
}
