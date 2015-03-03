package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.util.List;

public class ImportCSVIterator extends ImportIterator {
    List<List<String>> table;
    int current;
    
    public ImportCSVIterator(List<List<String>> table) {
        this.table = table;
        this.current = 0;
    }

    @Override
    public List<String> nextRow() {
        if (table.size() > current) {
            List<String> row = table.get(current);
            current++;
            return row;
        } else return null;
    }

    @Override
    protected void release() {
    }
}
