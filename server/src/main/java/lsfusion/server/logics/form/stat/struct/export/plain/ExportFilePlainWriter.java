package lsfusion.server.logics.form.stat.struct.export.plain;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;

import java.io.File;
import java.io.IOException;

public abstract class ExportFilePlainWriter extends ExportPlainWriter {
    
    protected final File file;

    public ExportFilePlainWriter(ImOrderMap<String, Type> fieldTypes) throws IOException {
        super(fieldTypes);
        file = File.createTempFile("file", ".exp");
    }

    public RawFileData release() throws IOException {
        RawFileData result;
        try {
            closeWriter();
        } finally {
            result = new RawFileData(file);
            BaseUtils.safeDelete(file);
        }
        return result;
    }

    protected abstract void closeWriter();
}
