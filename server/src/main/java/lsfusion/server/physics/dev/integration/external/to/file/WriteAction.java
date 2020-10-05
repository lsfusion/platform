package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.base.file.WriteUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class WriteAction extends SystemExplicitAction {
    private final Type sourcePropertyType;
    private boolean clientAction;
    private boolean dialog;
    private boolean append;

    public WriteAction(Type sourcePropertyType, boolean clientAction, boolean dialog, boolean append, ValueClass sourceProp, ValueClass pathProp) {
        super(sourceProp, pathProp);
        this.sourcePropertyType = sourcePropertyType;
        this.clientAction = clientAction;
        this.dialog = dialog;
        this.append = append;
    }

    @Override
    protected boolean allowNulls() {
        return false;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        DataObject sourceObject = context.getDataKeys().getValue(0);
        assert sourceObject.getType() instanceof FileClass;

        DataObject pathObject = context.getDataKeys().getValue(1);
        assert pathObject.getType() instanceof StringClass;
        String path = (String) pathObject.object;

        String extension = null;
        RawFileData rawFileData = null;
        if (sourceObject.object != null) {
            if (sourcePropertyType instanceof StaticFormatFileClass) {
                rawFileData = (RawFileData) sourceObject.object;
                extension = ((StaticFormatFileClass) sourcePropertyType).getOpenExtension(rawFileData);
            } else {
                extension = ((FileData) sourceObject.object).getExtension();
                rawFileData = ((FileData) sourceObject.object).getRawFile();
            }
        }
        try {
            if (rawFileData != null) {
                if (clientAction) {
                    if(append && dialog) {
                        throw new RuntimeException("APPEND is not supported in WRITE CLIENT DIALOG");
                    } else {
                        context.requestUserInteraction(new WriteClientAction(rawFileData, path, extension, append, dialog));
                    }
                } else {
                    WriteUtils.write(rawFileData, path, extension, false, append);
                }
            } else {
                throw new RuntimeException("File bytes not specified");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
