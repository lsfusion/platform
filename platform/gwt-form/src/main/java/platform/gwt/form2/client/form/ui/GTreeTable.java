package platform.gwt.form2.client.form.ui;

import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.SelectionChangeEvent;
import platform.gwt.form2.shared.view.GForm;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GTreeGridRecord;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.grid.GridEditableCell;

import java.util.*;

public class GTreeTable extends GGridPropertyTable {
    private GTreeTableTree tree;

    private List<String> createdFields = new ArrayList<String>();

    private GTreeGridRecord selectedRecord;

    private Set<GTreeTableNode> expandedNodes;

    public GTreeTable(GFormController iformController, GForm iform) {
        super(iformController);

        tree = new GTreeTableTree(iform);
        Column<GTreeGridRecord, Object> column = new Column<GTreeGridRecord, Object>(new GTreeGridControlCell(this)) {
            @Override
            public Object getValue(GTreeGridRecord object) {
                return object.getAttribute("treeColumn");
            }
        };
        GridHeader header = new GridHeader("Дерево");
        createdFields.add("treeColumn");
        headers.add(header);
        addColumn(column, header);
        setColumnWidth(column, "150px");

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                GTreeGridRecord selectedRecord = (GTreeGridRecord) selectionModel.getSelectedRecord();
                if (selectedRecord != null && !selectedRecord.equals(GTreeTable.this.selectedRecord)) {
                    setCurrentRecord(selectedRecord);
                    form.changeGroupObject(selectedRecord.getGroup(), selectedRecord.key);
                }
            }
        });
    }

    public void removeProperty(GGroupObject group, GPropertyDraw property) {
        dataUpdated = true;
        int index = tree.removeProperty(group, property);
        if (index > 0) {
            removeColumn(index);
//            hideField(property.sID);
        }
    }

    public void addProperty(GGroupObject group, GPropertyDraw property) {
        dataUpdated = true;

        int index = tree.addProperty(group, property);

        if (index > -1) {
            if (createdFields.contains(property.sID)) {
//                showField(property.sID);
            } else {
                GridHeader header = new GridHeader(property.getCaptionOrEmpty());
                Column<GTreeGridRecord, Object> gridColumn = createGridColumn(property);

                headers.add(index, header);
                insertColumn(index, gridColumn, header);
                createdFields.add(index, property.sID);

                setColumnWidth(gridColumn, "150px");
            }
        }
    }

    private Column<GTreeGridRecord, Object> createGridColumn(final GPropertyDraw property) {
        return new Column<GTreeGridRecord, Object>(new GridEditableCell(this)) {
            @Override
            public Object getValue(GTreeGridRecord record) {
                int row = currentRecords.indexOf(record);
                int column = tree.columnProperties.indexOf(property);
                return tree.getValue(getRowGroup(row), column, record.key);
            }
        };
    }

    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents) {
        tree.setKeys(group, keys, parents);
        dataUpdated = true;
    }

    public void setPropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        if (propValues != null) {
            dataUpdated = true;
            tree.setPropertyValues(property, propValues, updateKeys);
        }
    }

    public void update() {
        GTreeGridRecord selectedRecord = (GTreeGridRecord) selectionModel.getSelectedRecord();

        int oldKeyScrollTop = 0;
        GTreeGridRecord oldRecord = null;
        if (selectedRecord != null) {
            int oldKeyInd = currentRecords.indexOf(selectedRecord);

            if (oldKeyInd != -1) {
                oldRecord = selectedRecord;
                TableRowElement rowElement = getRowElement(oldKeyInd);
                oldKeyScrollTop = rowElement.getAbsoluteTop() - getScrollPanel().getAbsoluteTop();
            }
        }

        if (dataUpdated) {
            restoreVisualState();

            currentRecords = tree.getUpdatedRecords();
            setRowData(currentRecords);

            dataUpdated = false;
        }

        int currentInd = this.selectedRecord == null ? -1 : currentRecords.indexOf(this.selectedRecord);
        if (currentInd != -1) {
            if (this.selectedRecord.equals(oldRecord)) {
                scrollRowToVerticalPosition(currentInd, oldKeyScrollTop);
            } else {
                getRowElement(currentInd).scrollIntoView();
            }
            selectionModel.setSelected(currentRecords.get(currentInd), true);
            setKeyboardSelectedRow(currentInd, false);
        }

        updatePropertyReaders();

        updateHeader();
    }

    public void fireExpandNode(GTreeGridRecord record) {
        saveVisualState();
        GTreeTableNode node = tree.getNodeByRecord(record);
        if (node != null) {
            expandedNodes.add(node);
            form.expandGroupObject(node.getGroup(), node.getKey());
        }
    }

    public void fireCollapseNode(GTreeGridRecord record) {
        saveVisualState();
        GTreeTableNode node = tree.getNodeByRecord(record);
        if (node != null) {
            expandedNodes.remove(node);
            form.collapseGroupObject(node.getGroup(), node.getKey());
        }
    }

    public void saveVisualState() {
        expandedNodes = new HashSet<GTreeTableNode>();
        expandedNodes.addAll(getExpandedChildren(tree.root));
    }

    private List<GTreeTableNode> getExpandedChildren(GTreeTableNode node) {
        List<GTreeTableNode> exChildren = new ArrayList<GTreeTableNode>();
        for (GTreeTableNode child : node.getChildren()) {
            if (child.isOpen()) {
                exChildren.add(child);
                exChildren.addAll(getExpandedChildren(child));
            }
        }
        return exChildren;
    }

    public void restoreVisualState() {
        for (GTreeTableNode node :tree.root.getChildren()) {
            expandNode(node);
        }
    }

    private void setCurrentRecord(GTreeGridRecord record) {
        this.selectedRecord = record;
    }

    public GGroupObjectValue getCurrentKey() {
        return selectedRecord == null ? new GGroupObjectValue() : selectedRecord.key;
    }

    private void expandNode(GTreeTableNode node) {
        if (expandedNodes != null && expandedNodes.contains(node) && !tree.hasOnlyExpandningNodeAsChild(node)) {
            node.setOpen(true);
            for (GTreeTableNode child : node.getChildren()) {
                expandNode(child);
            }
        } else {
            node.setOpen(false);
        }
    }

    @Override
    public List<GPropertyDraw> getColumnProperties() {
        return tree.columnProperties;
    }

    @Override
    public void putValue(int row, int column, Object value) {
        tree.putValue(getProperty(row, column), getColumnKey(row, column), value);
    }

    @Override
    public int getColumnIndex(GPropertyDraw property) {
        return super.getColumnIndex(property) + 1;
    }

    private GGroupObject getRowGroup(int row) {
        return ((GTreeGridRecord) currentRecords.get(row)).getGroup();
    }

    @Override
    public Object getValueAt(int row, int column) {
        return tree.getValue(getRowGroup(row), column - 1, getColumnKey(row, column));
    }

    @Override
    public GPropertyDraw getProperty(int row, int column) {
        return tree.getProperty(getRowGroup(row), column - 1);
    }

    @Override
    public GGroupObjectValue getColumnKey(int row, int column) {
        return currentRecords.get(row).key;
    }
}
