package lsfusion.server.logics.property.actions.integration.exporting.plain;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class ExportByteArrayPlainWriter extends ExportPlainWriter {

    protected final ByteArrayOutputStream outputStream;

    public ExportByteArrayPlainWriter(ImOrderMap<String, Type> fieldTypes) throws IOException {
        super(fieldTypes);
        
        this.outputStream = new ByteArrayOutputStream();
    }

    public byte[] release() throws IOException {
        closeWriter();
        return outputStream.toByteArray();
    }

    protected abstract void closeWriter() throws IOException;
}
