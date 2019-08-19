package lsfusion.server.logics.form.stat.struct.export.plain;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportMatrixIterator;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainIterator;

import java.io.IOException;

// export of matrix format - columns A, B, ... and rows - 1, 2, ...
// can be with header (strict format) and without (flex format)
public abstract class ExportMatrixWriter extends ExportByteArrayPlainWriter {

    private ImSet<String> headerFields = null;
    protected final ImRevMap<Integer, String> fieldIndexMap;

    public ExportMatrixWriter(ImOrderMap<String, Type> fieldTypes, boolean noHeader) throws IOException {
        super(fieldTypes);
        
        if(noHeader)
            fieldIndexMap = getFieldIndexMap(fieldTypes).reverse();
        else {
            fieldIndexMap = fieldTypes.keyOrderSet().toIndexedMap();
            headerFields = fieldTypes.keys();
        }
    }

    protected void finalizeInit() throws IOException {
        if(headerFields != null) {
            writeLine(headerFields.toMap(), headerFields.toMap(ImportMatrixIterator.nameClass));
        }
    }

    protected static ImRevMap<String, Integer> getFieldIndexMap(ImOrderMap<String, Type> fieldTypes) {
        ImOrderMap<String, Integer> nameToIndex = ImportMatrixIterator.nameToIndexColumnsMapping;
        ImMap<String, String> mapping = ImportPlainIterator.getRequiredActualMap(nameToIndex.keyOrderSet(), fieldTypes, false);
        return mapping.toRevMap(fieldTypes.keyOrderSet()).join(nameToIndex.getMap().toRevExclMap());
    }
    
    @Override
    public void writeLine(ImMap<String, Object> row) throws IOException {
        writeLine(row, fieldTypes.getMap());
    }

    protected abstract void writeLine(ImMap<String, ?> values, ImMap<String, Type> types);
}
