package lsfusion.gwt.client.form.object.table.tree.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.JSNIHelper;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.EventHandler;
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
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableFooter;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static lsfusion.gwt.client.base.GwtClientUtils.setThemeImage;

public class GTreeTable extends GGridPropertyTable<GTreeGridRecord> {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    
    private boolean dataUpdated;

    private GTreeTableTree tree;

    private Set<GTreeTableNode> expandedNodes;

    private TreeTableSelectionHandler treeSelectionHandler;

    private GTreeGroupController treeGroupController;

    private GTreeGroup treeGroup;
    
    private boolean autoSize;

    private int hierarchicalWidth;

    public GTreeTable(GFormController iformController, GForm iform, GTreeGroupController treeGroupController, GTreeGroup treeGroup, boolean autoSize) {
        super(iformController, lastGroupObject(treeGroup), treeGroupController.getFont());

        this.treeGroupController = treeGroupController;
        this.treeGroup = treeGroup;
        this.autoSize = autoSize;

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

        getElement().setPropertyObject("groupObject", groupObject);

        if(treeGroupController.isExpandOnClick())
            form.addBinding(new GMouseInputEvent(GMouseInputEvent.DBLCLK)::isEvent, new GBindingEnv(100, GBindingMode.ONLY, null, GBindingMode.ONLY, null, null, null, null),
                    () -> isSelectedNodeExpandable(tree.getNodeByRecord(getSelectedRowValue())),
                    event -> {
                        GTreeTableNode node = tree.getNodeByRecord(getSelectedRowValue());
                        if (isSelectedNodeExpandable(node)) {
                            if (!node.isOpen()) {
                                fireExpandNode(node);
                            } else {
                                fireCollapseNode(node);
                            }
                        }
                    }, this, groupObject);
    }

    private static boolean isSelectedNodeExpandable(GTreeTableNode node) {
        return node != null && node.isExpandable();
    }

    private static GGroupObject lastGroupObject(GTreeGroup treeGroup) {
        return treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;
    }

    protected boolean isAutoSize() {
        return autoSize;
    }

    public void removeProperty(GPropertyDraw property) {
        dataUpdated = true;

        int index = tree.removeProperty(property);
        if(index > -1) // we need only last group, just like everywhere
            removeColumn(index);

        columnsUpdated = true;
    }

    public boolean isPropertyShown(GPropertyDraw property) {
        return tree.getPropertyIndex(property) != -1;
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        focus();
        int ind = tree.getPropertyIndex(propertyDraw);
        if (ind != -1) {
            changeSelectedColumn(ind);
        }
    }

    public void updateProperty(GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        dataUpdated = true;

        if(!updateKeys) {
            int index = tree.updateProperty(property);

            if (index > -1) {
                GridColumn gridColumn = new GridColumn(property);
                String propertyCaption = getPropertyCaption(property);
                GGridPropertyTableHeader header = noHeaders ? null : new GGridPropertyTableHeader(this, propertyCaption, property.getTooltipText(propertyCaption), gridColumn.isSticky());
                GGridPropertyTableFooter footer = property.hasFooter ? new GGridPropertyTableFooter(this, property, null, null) : null;

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
        GPropertyTableBuilder.setRowHeight(cellElement, 0, false); // somewhy it's needed for proper indent showing
        for (int i = 0; i <= treeValue.getLevel(); i++) {
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
        int nodeLevel = treeValue.getLevel();
        if (indentLevel < nodeLevel - 1) {
            indentIcon = treeValue.isLastInLevel(indentLevel) ? ICON_EMPTY : ICON_PASSBY;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else if (indentLevel == nodeLevel - 1) {
            indentIcon = ICON_BRANCH;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else {
            assert indentLevel == nodeLevel;
            img.setAttribute(TREE_NODE_ATTRIBUTE, treeValue.getSID());
            indentIcon = getNodeIcon(treeValue);
        }

        if (ICON_PASSBY.equals(indentIcon)) {
            changeDots(element, true, true);
        } else if (ICON_BRANCH.equals(indentIcon)) {
            if (treeValue.isLastInLevel(indentLevel)) {
                changeDots(element, true, false); //end
            } else {
                changeDots(element, true, true); //branch
            }
        } else if (ICON_EMPTY.equals(indentIcon) || ICON_LEAF.equals(indentIcon)) {
            changeDots(element, false, false);
        } else if (ICON_CLOSED.equals(indentIcon)) {
            changeDots(element, false, treeValue.isClosedDotBottom());
        }else if (ICON_OPEN.equals(indentIcon)) {
            changeDots(element, false, treeValue.isOpenDotBottom());
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

        setThemeImage(ICON_PASSBY.equals(indentIcon) ? ICON_EMPTY : indentIcon, img::setSrc);
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
        setThemeImage(ICON_PASSBY, str -> element.getStyle().setBackgroundImage("url('" + str + "')"));
    }

    private static void clearBackground(Element element) {
        element.removeClassName("passby-image");
        element.getStyle().clearBackgroundImage();
    }

    private static String getNodeIcon(GTreeColumnValue treeValue) {
        if (treeValue.getOpen() == null) {
            return ICON_LEAF;
        } else if (treeValue.getOpen()) {
            return ICON_OPEN;
        } else {
            return ICON_CLOSED;
        }
    }

    // actually singleton
    private class ExpandTreeColumn extends Column<GTreeGridRecord, Object> implements TreeGridColumn {

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
        public void onEditEvent(EventHandler handler, Cell editCell, Element editCellParent) {
            Event event = handler.event;
            boolean changeEvent = GMouseStroke.isChangeEvent(event);
            if (changeEvent || (treeGroupController.isExpandOnClick() && GMouseStroke.isDoubleChangeEvent(event))) { // we need to consume double click event to prevent treetable global dblclick binding (in this case node will be collapsed / expanded once again)
                String attrID = JSNIHelper.getAttributeOrNull(Element.as(event.getEventTarget()), TREE_NODE_ATTRIBUTE);
                if (attrID != null) {
                    if(changeEvent)
                        changeTreeState(editCell, getTreeValue(editCell), event);
                    handler.consume();
                }
            }
        }

        private void changeTreeState(Cell cell, Object value, NativeEvent event) {
            Boolean open = ((GTreeColumnValue) value).getOpen();
            if (open != null) {
                GTreeGridRecord record = getTreeGridRow(cell);
                if (!open) {
                    expandNodeByRecord(record);
                } else {
                    collapseNodeByRecord(record);
                }
            }
        }

        @Override
        public void renderAndUpdateDom(Cell cell, TableCellElement cellElement) {
            GTreeTable.renderExpandDom(cellElement, getTreeValue(cell));
        }

        @Override
        public void updateDom(Cell cell, TableCellElement cellElement) {

            GTreeColumnValue treeValue = getTreeValue(cell);

            while (cellElement.getChildCount() > treeValue.getLevel() + 1) {
                cellElement.getLastChild().removeFromParent();
            }

            for (int i = 0; i <= treeValue.getLevel(); i++) {
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
                    if(!GPropertyTableBuilder.clearRenderSized(cellElement, oldProperty, GTreeTable.this)) {
                        assert cellElement == GPropertyTableBuilder.getRenderSizedElement(cellElement, oldProperty, GTreeTable.this);
                        oldProperty.getCellRenderer().clearRender(cellElement, GTreeTable.this);
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

    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents, NativeHashMap<GGroupObjectValue, Boolean> expandable) {
        tree.setKeys(group, keys, parents, expandable);

        dataUpdated = true;
    }

    public void updatePropertyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        if (propValues != null) {
            dataUpdated = true;
            tree.setPropertyValues(property, propValues, updateKeys);
        }
    }

    public void updateReadOnlyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> readOnlyValues) {
        if (readOnlyValues != null) {
            tree.setReadOnlyValues(property, readOnlyValues);
        }
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
    private final NativeSIDMap<GPropertyDraw, Integer> userWidths = new NativeSIDMap<>();
    @Override
    protected void setUserWidth(GPropertyDraw property, Integer value) {
        userWidths.put(property, value);
    }

    @Override
    protected Integer getUserWidth(GPropertyDraw property) {
        return userWidths.get(property);
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


    public GTreeGridRecord getTreeGridRow(Cell editCell) {
        return (GTreeGridRecord) editCell.getRow();
    }
    protected TreeGridColumn getTreeGridColumn(int i) {
        return (TreeGridColumn) getColumn(i);
    }
    protected GridColumn getGridColumn(int i) {
        assert i > 0;
        return (GridColumn)getTreeGridColumn(i);
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
    protected int getHeaderHeight() {
        return treeGroup.headerHeight;
    }

    @Override
    protected boolean isColumnFlex(int i) {
        if(i == 0)
            return true;
        return super.isColumnFlex(i);
    }

    @Override
    protected int getColumnBaseWidth(int i) {
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
            restoreVisualState();

            checkUpdateCurrentRow();

//            checkSelectedRowVisible();

            rows = tree.updateRows(getColumnCount());
            updatePropertyReaders();
            treeSelectionHandler.dataUpdated();

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
        if (isVisible()) {
            super.onResize();
        }
    }

    protected void updatePropertyReaders() {
        for (GTreeGridRecord record : rows) {
            GGroupObjectValue key = record.getKey();

            Object rBackground = rowBackgroundValues.get(key);
            Object rForeground = rowForegroundValues.get(key);

            for (int i = 1, size = getColumnCount(); i < size; i++) {
                GPropertyDraw readerProperty = tree.getProperty(record.getGroup(), i);
                if(readerProperty != null) {

                    Object background = rBackground;
                    if (background == null) {
                        NativeHashMap<GGroupObjectValue, Object> propBackgrounds = cellBackgroundValues.get(readerProperty);
                        if (propBackgrounds != null) {
                            background = propBackgrounds.get(key);
                        }
                    }

                    Object foreground = rForeground;
                    if (foreground == null) {
                        NativeHashMap<GGroupObjectValue, Object> propForegrounds = cellForegroundValues.get(readerProperty);
                        if (propForegrounds != null) {
                            foreground = propForegrounds.get(key);
                        }
                    }

                    String columnSID = getColumnSID(i);
                    record.setBackground(columnSID, background == null ? readerProperty.background : background);
                    record.setForeground(columnSID, foreground == null ? readerProperty.foreground : foreground);
                    if (readerProperty.hasDynamicImage()) {
                        NativeHashMap<GGroupObjectValue, Object> actionImages = cellImages.get(readerProperty);
                        record.setImage(columnSID, actionImages == null ? null : actionImages.get(key));
                    }

                }
            }
        }
    }

    public void expandNodeByRecord(GTreeGridRecord record) {
        fireExpandNode(tree.getNodeByRecord(record));
    }

    public void fireExpandNodeRecursive(boolean current) {
        GTreeTableNode node = tree.getNodeByRecord(getSelectedRowValue());
        if (node != null) {
            saveVisualState();
            addExpandedNodes(current ? node : tree.root);
            form.expandGroupObjectRecursive(node.getGroup(), current);
        }
    }

    private void addExpandedNodes(GTreeTableNode node) {
        expandedNodes.add(node);
        for(GTreeTableNode child : node.getChildren()) {
            addExpandedNodes(child);
        }
    }

    public void fireExpandNode(GTreeTableNode node) {
        if (node != null) {
            saveVisualState();
            expandedNodes.add(node);
            form.expandGroupObject(node.getGroup(), node.getKey());
        }
    }

    public void collapseNodeByRecord(GTreeGridRecord record) {
        fireCollapseNode(tree.getNodeByRecord(record));
    }

    public void fireCollapseNodeRecursive(boolean current) {
        GTreeTableNode node = tree.getNodeByRecord(getSelectedRowValue());
        if (node != null) {
            saveVisualState();
            removeExpandedNodes(current ? node : tree.root);
            form.collapseGroupObjectRecursive(node.getGroup(), current);
        }
    }

    private void removeExpandedNodes(GTreeTableNode node) {
        expandedNodes.remove(node);
        for(GTreeTableNode child : node.getChildren()) {
            removeExpandedNodes(child);
        }
    }

    public boolean isCurrentPathExpanded() {
        GTreeTableNode node;
        GTreeGridRecord selectedRecord = getSelectedRowValue();
        return selectedRecord != null && (node = tree.getNodeByRecord(selectedRecord)) != null && node.isOpen();
    }

    private void fireCollapseNode(GTreeTableNode node) {
        if (node != null) {
            saveVisualState();
            expandedNodes.remove(node);
            form.collapseGroupObject(node.getGroup(), node.getKey());
        }
    }

    public void saveVisualState() {
        expandedNodes = new HashSet<>();
        expandedNodes.addAll(getExpandedChildren(tree.root));
    }

    private List<GTreeTableNode> getExpandedChildren(GTreeTableNode node) {
        List<GTreeTableNode> exChildren = new ArrayList<>();
        for (GTreeTableNode child : node.getChildren()) {
            if (child.isOpen()) {
                exChildren.add(child);
                exChildren.addAll(getExpandedChildren(child));
            }
        }
        return exChildren;
    }

    public void restoreVisualState() {
        for (GTreeTableNode node : tree.root.getChildren()) {
            expandNode(node);
        }
    }

    private void expandNode(GTreeTableNode node) {
        if (expandedNodes != null && expandedNodes.contains(node) && !tree.hasOnlyExpandingNodeAsChild(node)) {
            node.setOpen(true);
            for (GTreeTableNode child : node.getChildren()) {
                expandNode(child);
            }
        } else {
            node.setOpen(false);
        }
    }

    public GGroupObjectValue getSelectedKey() {
        GTreeGridRecord selectedRecord = getSelectedRowValue();
        return selectedRecord == null ? GGroupObjectValue.EMPTY : selectedRecord.getKey();
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
        List<GPropertyDraw> properties = tree.getProperties(selectedGroupObject);
        if (properties != null) {
            return properties.stream().map(property ->
                    getSelectedColumn(property, GGroupObjectValue.EMPTY)
            ).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public String getColumnSID(int column) {
        return "" + column;
    }

    @Override
    public GPropertyDraw getProperty(int row, int column) {
        GTreeGridRecord rowValue = getRowValue(row);
        return rowValue == null ? null : tree.getProperty(rowValue.getGroup(), column);
    }

    @Override
    public GPropertyDraw getProperty(Cell cell) {
        GTreeGridRecord rowValue = getTreeGridRow(cell);
        return rowValue == null ? null : tree.getProperty(rowValue.getGroup(), cell.getColumnIndex());
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
        GTreeGridRecord record = getTreeGridRow(cell);
        return record == null || tree.isReadOnly(record.getGroup(), cell.getColumnIndex(), record.getKey());
    }

    @Override
    public GGroupObjectValue getRowKey(Cell editCell) {
        return getTreeGridRow(editCell).getKey();
    }

    @Override
    public Object getValueAt(Cell cell) {
        GTreeGridRecord record = getTreeGridRow(cell);
        return record == null ? null : tree.getValue(record.getGroup(), cell.getColumnIndex(), record.getKey());
    }

    @Override
    public void quickFilter(Event event, GPropertyDraw filterProperty, GGroupObjectValue columnKey) {
        treeGroupController.quickEditFilter(event, filterProperty, columnKey);
    }

    @Override
    public void setValueAt(Cell cell, Object value) {
        GTreeGridRecord rowRecord = getTreeGridRow(cell);
        GPropertyDraw property = getProperty(cell);
        // assert property is not null since we want get here if property is null

        rowRecord.setValue(property, value);
        tree.values.get(property).put(rowRecord.getKey(), value);
    }

    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        return sortableHeaderManager.changeOrders(groupObject, orders, alreadySet);
    }

    public boolean keyboardNodeChangeState(boolean open) {
        GTreeTableNode node = tree.getNodeByRecord(getSelectedRowValue());
        if (node == null || !node.isExpandable()) {
            return false;
        }
        if (open) {
            if (!node.isOpen()) {
                treeSelectionHandler.nodeTryingToExpand = node;
                fireExpandNode(node);
                return true;
            }
        } else if (node.isOpen()) {
            fireCollapseNode(node);
            return true;
        }
        return false;
    }

    public class TreeTableSelectionHandler extends GridPropertyTableSelectionHandler<GTreeGridRecord> {
        public GTreeTableNode nodeTryingToExpand = null;

        public TreeTableSelectionHandler(DataGrid<GTreeGridRecord> table) {
            super(table);
        }

        public void dataUpdated() {
            if (nodeTryingToExpand != null) {
                if (!nodeTryingToExpand.isOpen()) {
                    nextColumn(true);
                }
                nodeTryingToExpand = null;
            }
        }

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
