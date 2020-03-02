package lsfusion.server.logics.form.stat.struct.imports.plain.table;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainIterator;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;

import java.io.IOException;

public class ImportTableIterator extends ImportPlainIterator {
    private final JDBCTable rs;

    public ImportTableIterator(ImOrderMap<String, Type> fieldTypes, RawFileData file, String wheres) throws IOException {
        super(fieldTypes, wheres);
        this.rs = JDBCTable.deserializeJDBC(file);
        
        finalizeInit();
    }

    protected ImOrderSet<String> readFields() {
        return rs.fields;
    }

    private int currentRow = 0;
    private ImMap<String, Object> row;
    @Override
    protected boolean nextRow(boolean checkWhere) {
        do {
            if(currentRow >= rs.set.size())
                return false;
            row = rs.set.get(currentRow++);
        } while (checkWhere && ignoreRow());
        return true;
    }

    @Override
    protected Object getPropValue(String name, Type type) {
        return type.read(row.get(name));
    }

    @Override
    protected Integer getRowIndex() {
        return currentRow;
    }

    @Override
    public void release() {
    }
}
