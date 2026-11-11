package lsfusion.gwt.client.form.object.table.grid.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.event.GChangeSelection;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.PValue;

import java.util.function.IntFunction;

// what a move makes of a grid's selection, for whoever draws the grid - the platform's table (GGridTable) and a React
// view (GReactFormData.GridRowsPart): of its rows, a range the server keeps, of which the grid shows at once what it
// has; and of its columns, a range the grid keeps itself, between the column it starts from and the one it ends on
public final class GGridSelection {

    // the rows whose selection a move from prevRow to row changes, of the rows the grid has (rowKey), by what it has
    // of them as selected (rowSelectValues): a range anew (MOVE) leaves none selected; a range run on (MOVEEND) takes
    // each row on the way as the next one is not, the row moved to selected - so it grows and shrinks
    public static NativeHashMap<GGroupObjectValue, PValue> getRowChanges(NativeHashMap<GGroupObjectValue, PValue> rowSelectValues, IntFunction<GGroupObjectValue> rowKey,
                                                                         int row, int prevRow, GChangeSelection changeSelection) {
        NativeHashMap<GGroupObjectValue, PValue> changes = new NativeHashMap<>();
        if (changeSelection == GChangeSelection.MOVE) {
            rowSelectValues.foreachEntry((key, value) -> { if(GridDataRecord.isRowSelect(value)) changes.put(key, GridDataRecord.invertSelect(value)); });
        } else {
            int dir = row >= prevRow ? 1 : -1;
            for (int i = prevRow; (dir > 0 ? i <= row : i >= row); i += dir) {
                GGroupObjectValue key = rowKey.apply(i);
                PValue selectValue = GridDataRecord.invertSelect(i == row ? null : rowSelectValues.get(rowKey.apply(i + dir)));
                if(!GwtClientUtils.nullEquals(rowSelectValues.get(key), selectValue))
                    changes.put(key, selectValue);
            }
        }
        return changes;
    }

    // the column a range starts from once a move makes a column its end: that column where a range starts anew (MOVE)
    // or there is none yet (-1), else where it started
    public static int getStartColumn(int startSelectColumn, int column, GChangeSelection changeSelection) {
        return startSelectColumn < 0 || changeSelection == GChangeSelection.MOVE ? column : startSelectColumn;
    }

    public static boolean isColumnSelected(int startSelectColumn, int endSelectColumn, int column) {
        return startSelectColumn >= 0 && column >= Math.min(startSelectColumn, endSelectColumn) && column <= Math.max(startSelectColumn, endSelectColumn);
    }

    public interface ColumnChange {
        void accept(int column, boolean set);
    }
    // the columns a change of the range changes: those it leaves out, then those it takes in - a range keeps the
    // direction it was made in, so its bounds can be reversed
    public static void forColumnChanges(int startSelectColumn, int endSelectColumn, int newStart, int newEnd, ColumnChange change) {
        if (startSelectColumn >= 0)
            for (int i = Math.min(startSelectColumn, endSelectColumn); i <= Math.max(startSelectColumn, endSelectColumn); i++)
                if (!isColumnSelected(newStart, newEnd, i))
                    change.accept(i, false);
        if (newStart >= 0)
            for (int i = Math.min(newStart, newEnd); i <= Math.max(newStart, newEnd); i++)
                if (!isColumnSelected(startSelectColumn, endSelectColumn, i))
                    change.accept(i, true);
    }
}
