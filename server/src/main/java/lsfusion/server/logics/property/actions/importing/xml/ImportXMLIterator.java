package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.util.List;

public class ImportXMLIterator extends ImportIterator {
    List<List<String>> table;
    int current;
    
    public ImportXMLIterator(List<List<String>> table) {
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
