package lsfusion.server.logics.property.actions.integration.exporting.plain;

import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.Type;

import java.io.File;
import java.io.IOException;

public abstract class ExportFilePlainWriter extends ExportPlainWriter {
    
    protected final File file;

    public ExportFilePlainWriter(ImOrderMap<String, Type> fieldTypes) throws IOException {
        super(fieldTypes);
        file = File.createTempFile("file", ".exp");
    }

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
