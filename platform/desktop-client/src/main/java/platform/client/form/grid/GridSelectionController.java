package platform.client.form.grid;

import platform.base.BaseUtils;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientTypeSerializer;

import java.text.ParseException;
import java.util.*;

public class GridSelectionController {
    private GridTable table;
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> selectedCells = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private List<ClientGroupObjectValue> gridKeys = new ArrayList<ClientGroupObjectValue>();
    private Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> temporaryValues = new LinkedHashMap<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>>();
    private boolean temporarySelectionAddition;
    private boolean directionDown;

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> mergedSelection = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    private ClientPropertyDraw firstColumn;
    private ClientPropertyDraw lastColumn;

    public GridSelectionController(GridTable table) {
        this.table = table;
        resetSelection();
    }

    private void commitSelection() {
        selectedCells = mergedSelection;
    }

    private List<ClientGroupObjectValue> getRowKeys() {
        return table.getRowKeys();
    }

    private List<ClientPropertyDraw> getProperties() {
        return table.getVisibleProperties();
    }

    private int indexOf(ClientPropertyDraw property) {
        return table.getModel().getPropertyIndex(property, null);
    }

    public void addProperty(ClientPropertyDraw newProperty) {
        if (!selectedCells.containsKey(newProperty)) {
            selectedCells.put(newProperty, new HashMap<ClientGroupObjectValue, Object>());
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        if (firstColumn == lastColumn && firstColumn == property) {
            firstColumn = null;
            lastColumn = null;
            return;
        }
        int removeIndex = getProperties().indexOf(property);
        if (property == firstColumn) {
            int newIndex = indexOf(firstColumn) > indexOf(lastColumn) ? removeIndex - 1 : removeIndex + 1;
            firstColumn = getProperties().get(newIndex);
        } else if (property == lastColumn) {
            int newIndex = indexOf(lastColumn) > indexOf(firstColumn) ? removeIndex - 1 : removeIndex + 1;
            lastColumn = getProperties().get(newIndex);
        }
    }

    private void mergeSelections() {
        int firstColumnIndex = indexOf(firstColumn);
        int lastColumnIndex = indexOf(lastColumn);
        if (temporaryValues.isEmpty() ||  firstColumnIndex == -1 || lastColumnIndex == -1) {
            mergedSelection = selectedCells;
            return;
        }

        Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> newMap = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>(selectedCells);
        for (int column = Math.min(firstColumnIndex, lastColumnIndex); column <= Math.max(firstColumnIndex, lastColumnIndex); column++) {
            ClientPropertyDraw property = table.getProperty(0, column);
            Map<ClientGroupObjectValue, Object> valueMap = selectedCells.get(property) != null ?
                    new HashMap<ClientGroupObjectValue, Object>(selectedCells.get(property)) :
                    new HashMap<ClientGroupObjectValue, Object>();
            for (ClientGroupObjectValue key : temporaryValues.keySet()) {
                if (temporaryValues.containsKey(key) && temporaryValues.get(key).containsKey(property)) {
                    if (temporarySelectionAddition) {
                        valueMap.put(key, temporaryValues.get(key).get(property));
                    } else {
                        valueMap.remove(key);
                    }
                }
                newMap.put(property, valueMap);
            }
        }
        mergedSelection = newMap;
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        lastColumn = table.getProperty(0, columnIndex);
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
                ClientPropertyDraw selProp = table.getProperty(rowIndex, columnIndex);
                temporarySelectionAddition = selectedCells.get(selProp) != null && selectedCells.get(selProp).containsKey(getRowKeys().get(rowIndex));
                addToTemporaryValues(rowIndex);
            }
        } else {
            if (extend) {
                //если без shift, resetSelection() уже сработал в !toggle && !extend
                modifyTemporaryValues(rowIndex);
            } else {
                firstColumn = table.getProperty(0, columnIndex);
                temporarySelectionAddition = true;
                resetSelection();
                if (table.getTableModel().getRowCount() != 0) {
                    addToSelection(newProperty, rowIndex);
                    addToTemporaryValues(rowIndex);
                }
            }
        }
    }

    private void addToSelection(ClientPropertyDraw property, int rowIndex) {
        if (selectedCells.get(property) != null) {
            selectedCells.get(property).put(getRowKeys().get(rowIndex), table.getValueAt(rowIndex, indexOf(property)));
        } else {
            resetSelection();
        }
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
            mergeSelections();
        }
    }

    private boolean removeFromSelection(ClientPropertyDraw property, int rowIndex) {
        if (selectedCells.get(property) != null && selectedCells.get(property).containsKey(getRowKeys().get(rowIndex))) {
            selectedCells.get(property).remove(getRowKeys().get(rowIndex));
            return true;
        }
        return false;
    }

    private void resetTemporarySelection() {
        temporaryValues.clear();
        mergeSelections();
    }

    private void addToTemporaryValues(int rowIndex) {
        Map<ClientPropertyDraw, Object> valueMap = new HashMap<ClientPropertyDraw, Object>();
        for (ClientPropertyDraw property : getProperties()) {
            valueMap.put(property, table.getValueAt(rowIndex, indexOf(property)));
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
        return mergedSelection.get(property) != null && mergedSelection.get(property).containsKey(key);
    }

    public void mousePressed(int firstColumnSelectionIndex) {
        firstColumn = table.getProperty(0, firstColumnSelectionIndex);
    }

    public void mouseReleased() {
        commitSelection();
    }

    public void recordingStarted(int column) {
        firstColumn = table.getProperty(0, column);
    }

    public void submitShiftSelection(Map<ClientGroupObjectValue, Map<ClientPropertyDraw, Object>> recording) {
        temporaryValues = recording;
        mergeSelections();
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
            if (movedDown) {
                start = getRowKeys().indexOf(gridKeys.get(gridKeys.size() - 1)) + 1;
                end = getRowKeys().size() - 1;
            } else {
                start = 0;
                end = getRowKeys().indexOf(gridKeys.get(0)) - 1;
            }
        }
        if (start == -1 || end == -1)
            return;
        List<ClientGroupObjectValue> differenceMap = new ArrayList<ClientGroupObjectValue>();
        for (int i = start; i <= end; i++) {
            differenceMap.add(getRowKeys().get(i));
        }

        if (!gridKeys.containsAll(differenceMap))
            if (movedDown) {
                gridKeys.addAll(differenceMap);
            } else {
                differenceMap.addAll(gridKeys);
                gridKeys = differenceMap;
            }
    }

    private Object modifyIfString(Object value, boolean multiline) {
        if (value != null && value instanceof String) {
            value = BaseUtils.rtrim((String) value);
            if (multiline) {
                if (((String) value).contains("\n") || ((String) value).contains("\t")) {
                    value = "\"" + ((String) value).replace("\"", "\"\"") + "\"";
                }
            } else {
                value = ((String) value).replaceAll("\n", " ");
                value = ((String) value).replaceAll("\t", " ");
            }
        }
        return value;
    }

    public Map<ClientPropertyDraw, List<ClientGroupObjectValue>> getSelectedCells() {
        Map<ClientPropertyDraw, List<ClientGroupObjectValue>> cellsMap = new HashMap<ClientPropertyDraw, List<ClientGroupObjectValue>>();
        for (ClientPropertyDraw propertyDraw : selectedCells.keySet()) {
            List<ClientGroupObjectValue> keys = new ArrayList<ClientGroupObjectValue>();
            for (ClientGroupObjectValue key : selectedCells.get(propertyDraw).keySet()) {
                keys.add(key);
            }
            if (!keys.isEmpty()) {
                cellsMap.put(propertyDraw, keys);
            }
        }
        return cellsMap;
    }

    public boolean hasSingleSelection() {
        int selectedCellsCount = 0;
        for (ClientPropertyDraw property : selectedCells.keySet()) {
            selectedCellsCount += selectedCells.get(property).size();
        }
        //сейчас возвращает true, даже если селекшн совсем пустой. сделано для более красивой работы CTRL+C/CTRL+V -
        //там при пустом выделении используется в качестве выделенной фокусная ячейка
        return selectedCellsCount <= 1;
    }

    public String getSelectedTableString() throws ParseException {
        commitSelection();

        if (selectedCells.isEmpty())
            return null;

        int firstPropertyIndex = -1, lastPropertyIndex = -1;
        for (int i = 0; i < getProperties().size(); i++) {
            if (selectedCells.get(getProperties().get(i)) != null && !selectedCells.get(getProperties().get(i)).isEmpty()) {
                if (firstPropertyIndex == -1)
                    firstPropertyIndex = i;
                lastPropertyIndex = i;
            }
        }

        //если выделена одна ячейка (или ни одной) и нажали CTRL+C, копируем текущее значение
        if (hasSingleSelection() && firstPropertyIndex != -1) {
            Object value = modifyIfString(table.getSelectedValue(getProperties().get(firstPropertyIndex), null), false);
            ClientPropertyDraw property = getProperties().get(firstPropertyIndex);
            return value == null ? "" : property.formatString(value);
        }

        if (firstPropertyIndex == -1 || lastPropertyIndex == -1)
            return null;

        String str = "";

        for (ClientGroupObjectValue key : gridKeys) {
            boolean addString = false;
            String rowString = "";
            for (int i = firstPropertyIndex; i <= lastPropertyIndex; i++) {
                ClientPropertyDraw property = getProperties().get(i);
                Object value = null;
                if (selectedCells.get(property).containsKey(key)) {
                    addString = true;
                    value = modifyIfString(selectedCells.get(property).get(key), true);
                }
                rowString += (value == null ? "" : property.formatString(value));
                if (i < lastPropertyIndex) {
                    rowString += "\t";
                }
            }
            if(addString) str +=rowString +"\n";
        }

        return str;
    }

    public int getQuantity() {
        int quantity = 0;
        for (ClientPropertyDraw property : mergedSelection.keySet()) {
            quantity += mergedSelection.get(property).size();
        }
        return quantity;
    }

    public int getNumbersQuantity() {
        int quantity = 0;
        for (ClientPropertyDraw property : mergedSelection.keySet()) {
            for (ClientGroupObjectValue key : mergedSelection.get(property).keySet()) {
                if (mergedSelection.get(property).get(key) instanceof Number) {
                    quantity++;
                }
            }
        }
        return quantity;
    }

    public Double getSum() {
        double sum = 0;
        for (ClientPropertyDraw property : mergedSelection.keySet()) {
            for (ClientGroupObjectValue key : mergedSelection.get(property).keySet()) {
                Object value = mergedSelection.get(property).get(key);
                if (value instanceof Number) {
                    sum += ((Number) value).doubleValue();
                }
            }
        }
        return sum;
    }
}
