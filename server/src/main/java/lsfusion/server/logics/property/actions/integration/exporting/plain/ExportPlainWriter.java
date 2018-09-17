package lsfusion.server.logics.property.actions.integration.exporting.plain;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.JDBFException;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.Type;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExportPlainWriter {

    protected final ImOrderMap<String, Type> fieldTypes;
    
    protected final File file;

    public ExportPlainWriter(ImOrderMap<String, Type> fieldTypes) throws IOException {
        this.fieldTypes = fieldTypes;
        file = File.createTempFile("file", ".exp");
    }

    public void writeCount(int count) throws IOException { // if needed        
    } 
    public abstract void writeLine(ImMap<String, Object> row) throws IOException; // fields needed if there are no headers, and import uses format's field ordering

    public byte[] release() throws IOException {
        byte[] result;
        try {
            closeWriter();
        } finally {
            result = IOUtils.getFileBytes(file);
            if(!file.delete())
                file.deleteOnExit();
        }
        return result;
    }
    
    protected abstract void closeWriter() throws IOException;
}