package lsfusion.server.logics.property.actions.integration.exporting.plain;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.JDBFException;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.Type;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExportPlainWriter {

    protected final ImOrderMap<String, Type> fieldTypes;
    
    protected final ByteArrayOutputStream outputStream;

    public ExportPlainWriter(ImOrderMap<String, Type> fieldTypes) throws IOException {
        this.fieldTypes = fieldTypes;
        outputStream = new ByteArrayOutputStream();
    }

    public void writeCount(int count) throws IOException { // if needed        
    } 
    public abstract void writeLine(ImMap<String, Object> row) throws IOException; // fields needed if there are no headers, and import uses format's field ordering

    public byte[] release() throws IOException {
        closeWriter();
        return outputStream.toByteArray();
    }
    
    protected abstract void closeWriter() throws IOException;
}