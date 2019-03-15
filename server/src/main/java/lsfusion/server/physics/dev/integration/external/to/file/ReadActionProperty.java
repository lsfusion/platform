package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.ReadClientAction;
import lsfusion.base.file.ReadUtils;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.classes.DynamicFormatFileClass;
import lsfusion.server.logics.classes.StringClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class ReadActionProperty extends SystemExplicitAction {
    private final ExtraReadProcessor extraReadProcessor;
    private final LP<?> targetProp;
    private final boolean clientAction;
    private final boolean dialog;

    public ReadActionProperty(ValueClass sourceProp, LP<?> targetProp, boolean clientAction, boolean dialog) {
        super(sourceProp);
        this.extraReadProcessor = new ExtraReadProcessor();
        this.targetProp = targetProp;
        this.clientAction = clientAction;
        this.dialog = dialog;

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Class.forName("com.informix.jdbc.IfxDriver");
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return false;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject sourceProp = context.getSingleDataKeyValue();
        assert sourceProp.getType() instanceof StringClass;
        String sourcePath = (String) sourceProp.object;

        try {

            boolean isDynamicFormatFileClass = targetProp.property.getType() instanceof DynamicFormatFileClass;
            boolean isBlockingFileRead = Settings.get().isBlockingFileRead();
            ReadUtils.ReadResult readResult;
            if (clientAction) {
                readResult = (ReadUtils.ReadResult) context.requestUserInteraction(new ReadClientAction(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, dialog));
            } else {
                readResult = ReadUtils.readFile(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, false, extraReadProcessor);
            }
            if (readResult != null) {
                if(isDynamicFormatFileClass)
                    targetProp.change((FileData)readResult.fileBytes, context);
                else
                    targetProp.change((RawFileData)readResult.fileBytes, context);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeProps(targetProp.property);
    }
}
