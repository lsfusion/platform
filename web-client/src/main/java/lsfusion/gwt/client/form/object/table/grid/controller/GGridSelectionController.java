package lsfusion.gwt.client.form.object.table.grid.controller;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;

// THE SELECTION OF A GRID'S ROWS AS WHAT DRAWS THE ROWS KEEPS IT - the rows a React view draws
// (GReactFormData.GridRowsPart), or the platform's table (GGridTable) -: what the calls that state the selection and
// its clipboard, a view's member's and the form's controller's (GReactFormData.changeGridObject and on), ask of it
public interface GGridSelectionController {
    // its rows, each by its place: the current one, and what it has of them as selected - the server's, shown at once
    int getRowCount();
    GGroupObjectValue getRowKey(int row);
    int getRow(GGroupObjectValue key);
    int getCurrentRow();
    boolean hasRowSelection();
    NativeHashMap<GGroupObjectValue, PValue> getRowSelectValues();
    void updateRowSelectValues(NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys);

    // its columns, each by its place: the one a call names, by its property's name, the current one, and the range
    // selected, its own - what a change of the range changes of the columns the form reads as selected
    int getColumnCount();
    GPropertyDraw getProperty(int column);
    int readColumn(String errorPrefix, String name);
    int getSelectedColumn();
    void updateCurrentColumn(int column);
    int getStartSelectColumn();
    int getEndSelectColumn();
    ArrayList<GGridTable.ColumnSelection> updateColumnSelection(int newStart, int newEnd);

    // its cells, as the clipboard takes them: whether a column's are any of them, the text of one as it is copied,
    // and the pattern a value pasted into one is read by - of a row it has not too
    boolean isClipboardColumn(int column);
    String getCopy(int row, int column);
    String getPattern(int row, int column);
}
