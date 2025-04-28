package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.*;

public class MakeUnzipFileAction extends InternalAction {

    public MakeUnzipFileAction(UtilsLogicsModule LM) {
        super(LM);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            FileData unzippingFile = (FileData) findProperty("unzipping[]").read(context);
            if (unzippingFile != null) {
                RawFileData file = unzippingFile.getRawFile();
                String extension = unzippingFile.getExtension();
                Map<String, FileData> result = ZipUtils.unpackFile(file, extension, true);
                for (Map.Entry<String, FileData> entry : result.entrySet()) {
                    findProperty("unzipped[STRING[100]]").change(entry.getValue(), context, new DataObject(entry.getKey()));
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}