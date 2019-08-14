package lsfusion.server.logics.form.stat.struct.export.plain;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;

import java.io.IOException;

public abstract class ExportPlainWriter {

    protected final ImOrderMap<String, Type> fieldTypes;
    
    public ExportPlainWriter(ImOrderMap<String, Type> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }

    public abstract RawFileData release() throws IOException;

    public void writeCount(int count) throws IOException { // if needed        
    } 
    public abstract void writeLine(ImMap<String, Object> row) throws IOException; // fields needed if there are no headers, and import uses format's field ordering
}