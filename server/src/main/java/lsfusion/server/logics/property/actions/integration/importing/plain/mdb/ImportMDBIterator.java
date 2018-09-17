package lsfusion.server.logics.property.actions.integration.importing.plain.mdb;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainIterator;

import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ImportMDBIterator extends ImportPlainIterator {
    Iterator<Map<String, Object>> rowsIterator;

    public ImportMDBIterator(ImOrderMap<String, Type> fieldTypes, byte[] file) throws IOException, ClassNotFoundException {
        super(fieldTypes);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) BaseUtils.deserializeCustomObject(file);
        this.rowsIterator = rows.iterator();
        
        finalizeInit();
    }

    @Override
    public ImMap<String, Object> next() {
        if (rowsIterator.hasNext()) {
            return MapFact.fromJavaMap(rowsIterator.next());
        }
        return null;
    }

    @Override
    protected ImOrderSet<String> readFields() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean nextRow() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object getPropValue(String name, Type type) throws lsfusion.server.data.type.ParseException, ParseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release() {
    }
}
