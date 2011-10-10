package platform.client.form.grid;

import platform.base.BaseUtils;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import java.util.*;

public class GridSelectionController {
    private GridTable table;
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> selectedCells = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private List<ClientGroupObjectValue> gridKeys = new ArrayList<ClientGroupObjectValue>();
    private Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> temporaryValues = new LinkedHashMap<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>>();
    private boolean temporarySelectionAddition;
    private boolean directionDown;

    private int firstColumn;
    private int lastColumn;

    public GridSelectionController(GridTable table) {
        this.table = table;
        resetSelection();
    }

    private void commitSelection() {
        selectedCells = mergeSelections();
    }

    private List<ClientGroupObjectValue> getRowKeys() {
        return table.getRowKeys();
    }

    private List<ClientPropertyDraw> getProperties() {
        return table.getProperties();
    }

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> mergeSelections() {
        if (temporaryValues.isEmpty() || firstColumn == -1)
            return selectedCells;

        Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> newMap = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>(selectedCells);
        for (int column = Math.min(firstColumn, lastColumn); column <= Math.max(firstColumn, lastColumn); column++) {
            ClientPropertyDraw property = table.getProperty(0, column);
            Map<ClientGroupObjectValue, Object> valueMap = new HashMap<ClientGroupObjectValue, Object>(selectedCells.get(property));
            for (ClientGroupObjectValue key : temporaryValues.keySet()) {
                if (temporaryValues.containsKey(key) && temporaryValues.get(key).containsKey(property)) {
                    if (temporarySelectionAddition) {
                        valueMap.put(key, temporaryValues.get(key).get(property));
                    } else {
                        valueMap.remove(key);
                    }
                }
            }
            newMap.put(property, valueMap);
        }
        return newMap;
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        lastColumn = columnIndex;
        directionDown = rowIndex > table.getSelectedRow();
        ClientPropertyDraw newProperty = table.getProperty(rowIndex, columnIndex);

        if (toggle) {
            if (extend) {
                //temporarySelectionAddition равен значению выделенности первой ячейки, т.к. уже сработал toggle && !extend
                modifyTemporaryValues(rowIndex);
            } else {
                resetTemporarySelection();
                if (!removeFromSelection(newProperty, rowIndex)) {
                    addToSelection(newProperty, rowIndex);
                }
                addToTemporaryValues(rowIndex);
                temporarySelectionAddition = selectedCells.get(table.getProperty(rowIndex, columnIndex)).containsKey(getRowKeys().get(rowIndex));
            }
        } else {
            if (extend) {
                //если без shift, resetSelection() уже сработал в !toggle && !extend
                    modifyTemporaryValues(rowIndex);
            } else {
                firstColumn = columnIndex;
                temporarySelectionAddition = true;
                resetSelection();
                addToTemporaryValues(rowIndex);
            }
        }
    }

    private void addToSelection(ClientPropertyDraw property, int rowIndex) {
        selectedCells.get(property).put(getRowKeys().get(rowIndex), table.getValueAt(rowIndex, table.getModel().getPropertyIndex(property, null)));
    }

    private void modifyTemporaryValues(int currentRow) {
        int previousRow = table.getSelectedRow();
        if (currentRow != -1 && previousRow != -1) {
            int start = temporaryValues.isEmpty() ? previousRow : table.getRowKeys().indexOf(BaseUtils.lastSetElement(temporaryValues.keySet()));
            start = directionDown ? start + 1 : start - 1;

            Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> segment = new LinkedHashMap<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>>();
            for (int i = start; (directionDown && i <= currentRow) || (!directionDown && i >= currentRow); i = (directionDown ? i + 1 : i - 1)) {
                segment.put(table.getRowKeys().get(i), KeyController.getRowData(table, i));
            }

            temporaryValues = KeyController.mergeSelectionMaps(temporaryValues, segment);
        }
    }

    private boolean removeFromSelection(ClientPropertyDraw property, int rowIndex) {
        if (selectedCells.get(property).containsKey(getRowKeys().get(rowIndex))) {
            selectedCells.get(property).remove(getRowKeys().get(rowIndex));
            return true;
        }
        return false;
    }

    private void resetTemporarySelection() {
        temporaryValues.clear();
    }

    private void addToTemporaryValues(int rowIndex) {
        Map<ClientPropertyDraw, Object> valueMap = new HashMap<ClientPropertyDraw, Object>();
        for (ClientPropertyDraw property : getProperties()) {
            valueMap.put(property, table.getValueAt(rowIndex, table.getModel().getPropertyIndex(property, null)));
        }
        temporaryValues.put(getRowKeys().get(rowIndex), valueMap);
    }

    public void resetSelection() {
        resetTemporarySelection();
        selectedCells.clear();
        for (ClientPropertyDraw property : getProperties()) {
            selectedCells.put(property, new HashMap<ClientGroupObjectValue, Object>());
        }

        gridKeys = new ArrayList<ClientGroupObjectValue>();
        keysChanged(true);
    }

    public boolean isCellSelected(ClientPropertyDraw property, ClientGroupObjectValue key) {
        Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> selection = mergeSelections();
        return selection.get(property) != null && selection.get(property).containsKey(key);
    }

    public void mousePressed(int firstColumnSelectionIndex) {
        firstColumn = firstColumnSelectionIndex;
    }

    public void mouseReleased() {
        commitSelection();
    }

    public void recordingStarted(int column) {
        firstColumn = column;
    }

    public void submitShiftSelection(Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> recording) {
        temporaryValues = recording;
    }

    public void recordingStopped() {
        commitSelection();
    }

    public void keysChanged(boolean movedDown) {
        if (table.getRowCount() == 0 || gridKeys.containsAll(getRowKeys()))
            return;

        int start;
        int end;
        if (gridKeys.isEmpty()) {
            start = 0;
            end = getRowKeys().size() - 1;
        } else {
            int lastKeyIndex = getRowKeys().indexOf(gridKeys.get(gridKeys.size() - 1));
            if (movedDown) {
                start = lastKeyIndex;
                end = getRowKeys().size() - 1;
            } else {
                start = 0;
                end = lastKeyIndex;
            }
        }
        if (start == -1 || end == -1)
            return;
        List<ClientGroupObjectValue> differenceMap = new ArrayList<ClientGroupObjectValue>();
        for (int i = start; i <= end; i++) {
            differenceMap.add(getRowKeys().get(i));
        }

        if (movedDown) {
            gridKeys.addAll(differenceMap);
        } else {
            differenceMap.addAll(gridKeys);
            gridKeys = differenceMap;
        }
    }

    private Object trimIfString(Object value) {
        if (value != null && value instanceof String) {
            value = BaseUtils.rtrim((String) value);
        }
        return value;
    }

    public String getSelectedTableString() {
        int firstPropertyIndex = -1, lastPropertyIndex = -1;
        for (int i = 0; i < getProperties().size(); i++) {
            if (!selectedCells.get(getProperties().get(i)).isEmpty()) {
                if (firstPropertyIndex == -1)
                    firstPropertyIndex = i;
                lastPropertyIndex = i;
            }
        }

        if (firstPropertyIndex == -1 || lastPropertyIndex == -1)
            return null;

        //если выделена одна ячейка и нажали CTRL+C, копируем текуще значение
        if (firstPropertyIndex == lastPropertyIndex && (selectedCells.get(getProperties().get(firstPropertyIndex)).size() == 1)) {
            Object value = trimIfString(table.getSelectedValue(getProperties().get(firstPropertyIndex), null));
            return value == null ? "" : value.toString();
        }

        String str = "";
        for (ClientGroupObjectValue key : gridKeys) {
            String rowString = "";
            for (int i = firstPropertyIndex; i <= lastPropertyIndex; i++) {
                ClientPropertyDraw property = getProperties().get(i);
                Object value = null;
                if (selectedCells.get(property).containsKey(key)) {
                    value = trimIfString(selectedCells.get(property).get(key));
                }
                rowString += (value == null ? "" : value.toString());
                if (i < lastPropertyIndex) {
                    rowString += "\t";
                }
            }
            if (!BaseUtils.isRedundantString(rowString))
                str += rowString + "\n";
        }
        return str;
    }
}
