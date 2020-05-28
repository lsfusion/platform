package lsfusion.gwt.client.form.object.table.tree.view;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.KeyboardRowChangedEvent;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.base.view.grid.cell.CellPreviewEvent;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.property.cell.view.GridEditableCell;

import java.util.*;

import static java.util.Collections.singleton;

public class GTreeTable extends GGridPropertyTable<GTreeGridRecord> {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    
    private boolean dataUpdated;
    private boolean columnsUpdated;

    private GGroupObjectValue pathToSet;

    private ArrayList<GTreeGridRecord> currentRecords;

    private GTreeTableTree tree;

    private GTreeGridRecord selectedRecord;

    private Set<GTreeTableNode> expandedNodes;

    private TreeTableKeyboardSelectionHandler keyboardSelectionHandler;

    private GTreeGroupController treeGroupController;
    
    private boolean autoSize;

    private int hierarchicalWidth;

    public GTreeTable(GFormController iformController, GForm iform, GTreeGroupController treeGroupController, GTreeGroup treeGroup, boolean autoSize) {
        super(iformController, lastGroupObject(treeGroup), treeGroupController.getFont());

        this.treeGroupController = treeGroupController;
        this.autoSize = autoSize;

        tree = new GTreeTableTree(iform);

        Column<GTreeGridRecord, Object> column = new ExpandTreeColumn();
        GGridPropertyTableHeader header = new GGridPropertyTableHeader(this, messages.formTree(), null);
        addColumn(column, header);

        hierarchicalWidth = treeGroup.calculateSize();

        keyboardSelectionHandler = new TreeTableKeyboardSelectionHandler(this);
        setKeyboardSelectionHandler(keyboardSelectionHandler);

        addKeyboardRowChangedHandler(new KeyboardRowChangedEvent.Handler() {
            @Override
            public void onKeyboardRowChanged(KeyboardRowChangedEvent event) {
                final GTreeGridRecord kbSelectedRecord = getKeyboardSelectedRowValue();
                if (kbSelectedRecord != null && !kbSelectedRecord.equals(selectedRecord)) {
                    setCurrentRecord(kbSelectedRecord);
                    form.changeGroupObjectLater(kbSelectedRecord.getGroup(), kbSelectedRecord.getKey());
                }
            }
        });

        sortableHeaderManager = new GGridSortableHeaderManager<GPropertyDraw>(this, true) {
            @Override
            protected void orderChanged(GPropertyDraw columnKey, GOrder modiType, boolean alreadySet) {
                form.changePropertyOrder(columnKey, GGroupObjectValue.EMPTY, modiType, alreadySet);
            }

            @Override
            protected void ordersCleared(GGroupObject groupObject) {
                form.clearPropertyOrders(groupObject);
            }

            @Override
            protected GPropertyDraw getColumnKey(int column) {
                return getTreeGridColumn(column).getProperty();
            }
        };

        getElement().setPropertyObject("groupObject", groupObject);
    }

    private static GGroupObject lastGroupObject(GTreeGroup treeGroup) {
        return treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;
    }

    @Override
    protected boolean isAutoSize() {
        return autoSize;
    }

    public void removeProperty(GGroupObject group, GPropertyDraw property) {
        dataUpdated = true;

        int index = tree.removeProperty(group, property);
        if(index > -1) // we need only last group, just like everywhere
            removeColumn(index);

        columnsUpdated = true;
    }

    public void updateProperty(GGroupObject group, GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, HashMap<GGroupObjectValue, Object> values) {
        dataUpdated = true;

        if(!updateKeys) {
            int index = tree.updateProperty(group, property);

            if (index > -1) {
                GridColumn gridColumn = new GridColumn(property);
                String propertyCaption = property.getCaptionOrEmpty();
                GGridPropertyTableHeader header = new GGridPropertyTableHeader(this, propertyCaption, property.getTooltipText(propertyCaption));

                insertColumn(index, gridColumn, header);

                columnsUpdated = true;
            }
        }
        updatePropertyValues(property, values, updateKeys);
    }

    private abstract class TreeGridColumn extends Column<GTreeGridRecord, Object> {
        public TreeGridColumn(Cell<Object> cell) {
            super(cell);
        }

        public abstract GPropertyDraw getProperty();
    }

    // actually singleton
    private class ExpandTreeColumn extends TreeGridColumn {
        public ExpandTreeColumn() {
            super(new GTreeGridControlCell(GTreeTable.this));
        }

        @Override
        public Object getValue(GTreeGridRecord object) {
            return object.getTreeValue();
        }

        @Override
        public GPropertyDraw getProperty() {
            return null;
        }
    }

    private class GridColumn extends TreeGridColumn {

        public final GPropertyDraw property;

        public GridColumn(GPropertyDraw property) {
            super(new GridEditableCell(GTreeTable.this, true));

            this.property = property;
        }

        @Override
        public Object getValue(GTreeGridRecord record) {
            return record.getValue(property);
        }

        @Override
        public GPropertyDraw getProperty() {
            return property;
        }
    }

    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents, HashMap<GGroupObjectValue, Boolean> expandable) {
        tree.setKeys(group, keys, parents, expandable);
        dataUpdated = true;
        needToRestoreScrollPosition = true;
    }

    public void updatePropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        if (propValues != null) {
            dataUpdated = true;
            tree.setPropertyValues(property, propValues, updateKeys);
        }
    }

    public void updateReadOnlyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> readOnlyValues) {
        if (readOnlyValues != null) {
            tree.setReadOnlyValues(property, readOnlyValues);
        }
    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        super.updateCellBackgroundValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        super.updateCellForegroundValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        super.updateRowBackgroundValues(values);
        dataUpdated = true;
    }

    @Override
    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        super.updateRowForegroundValues(values);
        dataUpdated = true;
    }

    private Integer hierarchicalUserWidth = null;
    private final NativeHashMap<GPropertyDraw, Integer> userWidths = new NativeHashMap<>();
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

    protected TreeGridColumn getTreeGridColumn(int i) {
        return (TreeGridColumn) getColumn(i);
    }
    protected GridColumn getGridColumn(int i) {
        assert i > 0;
        return (GridColumn)getTreeGridColumn(i);
    }

    @Override
    protected GPropertyDraw getColumnPropertyDraw(int i) {
        return getGridColumn(i).property;
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
        storeScrollPosition();

        if(columnsUpdated) {
            updateLayoutWidth();
            columnsUpdated = false;
        }

        if (dataUpdated) {
            restoreVisualState();

            currentRecords = tree.getUpdatedRecords(getColumnCount(), i -> getColumnPropertyDraw(i));
            updatePropertyReaders();
            setRowData(currentRecords);

            keyboardSelectionHandler.dataUpdated();

            redraw();

            dataUpdated = false;
        }

        updateHeader();

        updateCurrentRecord();
    }

    public void updateCurrentRecord() {
        if (pathToSet != null) {
            GGroupObjectValue currentPath = pathToSet;
            pathToSet = null;
            if (currentRecords != null) {
                int i = 0;
                for (GTreeGridRecord record : currentRecords) {
                    if (record.getKey().equals(currentPath)) {
                        setCurrentRecord(record);
                        setKeyboardSelectedRow(i, false);
                        return;
                    }
                    i++;
                }
            }
        }
    }

    @Override
    public void onResize() {
        if (isVisible()) {
            super.onResize();
        }
    }

    protected void updatePropertyReaders() {
        if (rowBackgroundValues.size() != 0 || rowForegroundValues.size() != 0 || cellBackgroundValues.size() != 0 || cellForegroundValues.size() != 0) { // optimization
            for (GTreeGridRecord record : currentRecords) {
                GGroupObjectValue key = record.getKey();

                Object rBackground = rowBackgroundValues.get(key);
                Object rForeground = rowForegroundValues.get(key);

                for (int i = 1, size = getColumnCount(); i < size; i++) {
                    GPropertyDraw readerProperty = tree.getProperty(record.getGroup(), i);

                    Object background = rBackground;
                    if (background == null) {
                        Map<GGroupObjectValue, Object> propBackgrounds = cellBackgroundValues.get(readerProperty);
                        if (propBackgrounds != null) {
                            background = propBackgrounds.get(key);
                        }
                    }

                    Object foreground = rForeground;
                    if (foreground == null) {
                        Map<GGroupObjectValue, Object> propForegrounds = cellForegroundValues.get(readerProperty);
                        if (propForegrounds != null) {
                            foreground = propForegrounds.get(key);
                        }
                    }

                    record.setBackground(i, background == null ? readerProperty.background : background);
                    record.setForeground(i, foreground == null ? readerProperty.foreground : foreground);
                }
            }
        }
    }

    protected void updateHeader() {
        boolean needsHeaderRefresh = false;
        int rowHeight = 0;
        for (int i = 1, size = getColumnCount(); i < size; i++) {
            GPropertyDraw property = getColumnPropertyDraw(i);
            Map<GGroupObjectValue, Object> captions = propertyCaptions.get(property);
            if (captions != null) {
                String value = GwtSharedUtils.nullTrim(captions.values().iterator().next());
                GGridPropertyTableHeader header = getGridHeader(i);
                header.setCaption(value, false, false);
                header.setToolTip(property.getTooltipText(value));
                needsHeaderRefresh = true;
            }
            rowHeight = Math.max(rowHeight, property.getValueHeight(font));
        }
        setCellHeight(rowHeight);
        if (needsHeaderRefresh) {
            refreshHeaders();
        }
    }

    public void expandNodeByRecord(GTreeGridRecord record) {
        fireExpandNode(tree.getNodeByRecord(record));
    }

    public void fireExpandNodeRecursive(boolean current) {
        GTreeTableNode node = tree.getNodeByRecord(getSelectedRecord());
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
        GTreeTableNode node = tree.getNodeByRecord(getSelectedRecord());
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

    private void setCurrentRecord(GTreeGridRecord record) {
        Log.debug("Setting current record to: " + record);
        this.selectedRecord = record;
    }

    public void setCurrentPath(GGroupObjectValue currentPath) {
        Log.debug("Setting current path to: " + currentPath);
        this.pathToSet = currentPath;
    }

    public GTreeGridRecord getSelectedRecord() {
        return selectedRecord;
    }

    public GGroupObjectValue getCurrentKey() {
        return selectedRecord == null ? GGroupObjectValue.EMPTY : selectedRecord.getKey();
    }

    public GGroupObject getGroupObject() {
        return null;
    }

    @Override
    public GridPropertyTableKeyboardSelectionHandler getKeyboardSelectionHandler() {
        return keyboardSelectionHandler;
    }

    @Override
    public GAbstractTableController getGroupController() {
        return treeGroupController;
    }

    public GPropertyDraw getCurrentProperty() {
        GPropertyDraw property = getSelectedProperty();
        if (property == null && getColumnCount() > 1) {
            property = getColumnPropertyDraw(1);
        }
        return property;
    }

    public Object getSelectedValue(GPropertyDraw property) {
        GTreeGridRecord record = getSelectedRecord();
        return record == null ? null : record.getValue(property);
    }

    @Override
    public String getCellBackground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getBackground(column);
    }

    @Override
    public String getCellForeground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getForeground(column);
    }

    @Override
    public GPropertyDraw getProperty(Cell.Context context) {
        GTreeGridRecord rowValue = (GTreeGridRecord) context.getRowValue();
        return rowValue == null ? null : tree.getProperty(rowValue.getGroup(), context.getColumn());
    }

    @Override
    public GGroupObjectValue getSelectedColumn() {
        return null;
    }
    @Override
    public GGroupObjectValue getColumnKey(Cell.Context context) {
        return GGroupObjectValue.EMPTY;
//        return currentRecords.get(context.getIndex()).getKey();
    }

    @Override
    public boolean isEditable(Cell.Context context) {
        GTreeGridRecord record = (GTreeGridRecord) context.getRowValue();
        return record != null && tree.isEditable(record.getGroup(), context.getColumn(), record.getKey());
    }

    @Override
    public Object getValueAt(Cell.Context context) {
        GTreeGridRecord record = (GTreeGridRecord) context.getRowValue();
        return record == null ? null : tree.getValue(record.getGroup(), context.getColumn(), record.getKey());
    }

    @Override
    public void pasteData(List<List<String>> table) {
        if (!table.isEmpty() && !table.get(0).isEmpty()) {
            GPropertyDraw property = getSelectedProperty();
            GGroupObjectValue columnKey = getCurrentKey();
            if (property != null) {
                form.pasteSingleValue(property, columnKey, table.get(0).get(0));
            }
        }
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        super.onBrowserEvent2(event);

        if (event.getTypeInt() == Event.ONDBLCLICK) {
            if (treeGroupController.isExpandOnClick() && !isEditable(getCurrentCellContext()) && getTableBodyElement().isOrHasChild(Node.as(event.getEventTarget()))) {
                GTreeTableNode node = tree.getNodeByRecord(getSelectedRecord());
                if (node != null && node.isExpandable()) {
                    GwtClientUtils.stopPropagation(event);
                    if (!node.isOpen()) {
                        fireExpandNode(node);
                    } else {
                        fireCollapseNode(node);
                    }
                }
            }
        }
    }

    @Override
    public void quickFilter(EditEvent event, GPropertyDraw filterProperty, GGroupObjectValue columnKey) {
        treeGroupController.quickEditFilter(event, filterProperty, columnKey);
    }

    @Override
    public void setValueAt(Cell.Context context, Object value) {
        int row = context.getIndex();
        int column = context.getColumn();

        GTreeGridRecord rowRecord = (GTreeGridRecord) context.getRowValue();

        if (rowRecord != null) {
            GPropertyDraw property = getProperty(context);
            if (property != null) {
                rowRecord.setValue(property, value);
                tree.putValue(property, rowRecord.getKey(), value);
            }
        }

        setRowData(row, Arrays.asList(rowRecord));
        redrawColumns(singleton(getColumn(column)), false);
    }

    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        return sortableHeaderManager.changeOrders(groupObject, orders, alreadySet);
    }

    public boolean keyboardNodeChangeState(boolean open) {
        GTreeTableNode node = tree.getNodeByRecord(selectedRecord);
        if (node == null || !node.isExpandable()) {
            return false;
        }
        if (open) {
            if (!node.isOpen()) {
                keyboardSelectionHandler.nodeTryingToExpand = node;
                fireExpandNode(node);
                return true;
            }
        } else if (node.isOpen()) {
            fireCollapseNode(node);
            return true;
        }
        return false;
    }

    public class TreeTableKeyboardSelectionHandler extends GridPropertyTableKeyboardSelectionHandler<GTreeGridRecord> {
        public GTreeTableNode nodeTryingToExpand = null;

        public TreeTableKeyboardSelectionHandler(DataGrid<GTreeGridRecord> table) {
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
        public boolean handleKeyEvent(CellPreviewEvent<GTreeGridRecord> event) {
            NativeEvent nativeEvent = event.getNativeEvent();

            assert BrowserEvents.KEYDOWN.equals(nativeEvent.getType());

            int keyCode = nativeEvent.getKeyCode();
            if (!nativeEvent.getCtrlKey() && getKeyboardSelectedColumn() == 0) {
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
