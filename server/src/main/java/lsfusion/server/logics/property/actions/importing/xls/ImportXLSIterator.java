package lsfusion.server.logics.property.actions.importing.xls;

import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.util.List;

public class ImportXLSIterator extends ImportIterator {
    List<List<String>> table;
    int current;
    
    public ImportXLSIterator(List<List<String>> table) {
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
