package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.base.file.WriteUtils;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class WriteAction extends SystemAction {
    private final Type sourcePropertyType;
    private boolean clientAction;
    private boolean dialog;
    private boolean append;

    public WriteAction(Type sourcePropertyType, boolean clientAction, boolean dialog, boolean append) {
        super(LocalizedString.create("Read"), SetFact.toOrderExclSet(2, i -> new PropertyInterface()));
        this.sourcePropertyType = sourcePropertyType;
        this.clientAction = clientAction;
        this.dialog = dialog;
        this.append = append;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue sourceObject = context.getKeys().getValue(0);
        assert sourceObject.getType() instanceof FileClass;

        ObjectValue pathObject = context.getKeys().getValue(1);
        assert pathObject.getType() instanceof StringClass;

        FileData fileData = readFile(sourceObject, sourcePropertyType, ExternalUtils.resultCharset.toString());
        if(fileData != null && pathObject instanceof DataObject) {
            String path = (String) pathObject.getValue();
            try {
                String extension = fileData.getExtension();
                RawFileData rawFileData = fileData.getRawFile();
                if (clientAction) {
                    if (append && dialog) {
                        throw new RuntimeException("APPEND is not supported in WRITE CLIENT DIALOG");
                    } else {
                        context.requestUserInteraction(new WriteClientAction(rawFileData, path, extension, append, dialog));
                    }
                } else {
                    WriteUtils.write(rawFileData, path, extension, false, append);
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return FlowResult.FINISH;
    }
}
