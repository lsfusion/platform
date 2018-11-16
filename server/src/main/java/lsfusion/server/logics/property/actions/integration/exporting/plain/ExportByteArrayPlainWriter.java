package lsfusion.server.logics.property.actions.integration.exporting.plain;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.RawFileData;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class ExportByteArrayPlainWriter extends ExportPlainWriter {

    protected final ByteArrayOutputStream outputStream;

    public ExportByteArrayPlainWriter(ImOrderMap<String, Type> fieldTypes) throws IOException {
        super(fieldTypes);
        
        this.outputStream = new ByteArrayOutputStream();
    }

    public RawFileData release() throws IOException {
        closeWriter();
        return new RawFileData(outputStream);
    }

    protected abstract void closeWriter() throws IOException;
}
