package lsfusion.server.logics.property.actions.integration.exporting.plain;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportMatrixIterator;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainIterator;

import java.io.IOException;

// export of matrix format - columns A, B, ... and rows - 1, 2, ...
// can be with header (strict format) and without (flex format)
public abstract class ExportMatrixWriter extends ExportByteArrayPlainWriter {

    protected final ImRevMap<Integer, String> fieldIndexMap;

    public ExportMatrixWriter(ImOrderMap<String, Type> fieldTypes, boolean noHeader) throws IOException {
        super(fieldTypes);
        
        if(noHeader)
            fieldIndexMap = getFieldIndexMap(fieldTypes).reverse();
        else {
            fieldIndexMap = fieldTypes.keyOrderSet().toIndexedMap();
            
            ImSet<String> fields = fieldTypes.keys();
            writeLine(fields.toMap(), fields.toMap((Type) ImportMatrixIterator.nameClass));
        }
    }

    private static ImRevMap<String, Integer> getFieldIndexMap(ImOrderMap<String, Type> fieldTypes) {
        ImOrderMap<String, Integer> nameToIndex = ImportMatrixIterator.nameToIndexColumnsMapping;
        ImMap<String, String> mapping = ImportPlainIterator.getRequiredActualMap(nameToIndex.keyOrderSet(), fieldTypes, false);
        return mapping.toRevMap(fieldTypes.keyOrderSet()).join(nameToIndex.getMap().toRevExclMap());
    }
    
    @Override
    public void writeLine(ImMap<String, Object> row) throws IOException {
        writeLine(row, fieldTypes.getMap());
    }

    protected abstract void writeLine(ImMap<String, ?> values, ImMap<String, Type> types) throws IOException;
}
