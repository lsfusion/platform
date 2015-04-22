package lsfusion.server.logics.property.actions.importing.mdb;

import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImportMDBIterator extends ImportIterator {
    Iterator<List<String>> rowMapIterator;
    List<Integer> sourceColumns;

    public ImportMDBIterator(List<List<String>> rowsList, List<Integer> sourceColumns) {
        this.rowMapIterator = rowsList.iterator();
        this.sourceColumns = sourceColumns;
    }

    @Override
    public List<String> nextRow() {
        if (rowMapIterator.hasNext()) {
            List<String> row = rowMapIterator.next();
            List<String> listRow = new ArrayList<String>();
            for (Integer column : sourceColumns) {
                listRow.add(row.get(column));
            }
            return listRow;
        }
        return null;
    }

    @Override
    protected void release() {
    }
}
