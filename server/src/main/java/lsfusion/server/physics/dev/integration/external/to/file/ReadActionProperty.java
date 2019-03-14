package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.ReadClientAction;
import lsfusion.base.file.ReadUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.Settings;
import lsfusion.server.logics.classes.DynamicFormatFileClass;
import lsfusion.server.logics.classes.StringClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.DataObject;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitActionProperty;

import java.sql.SQLException;

public class ReadActionProperty extends SystemExplicitActionProperty {
    private final ExtraReadProcessor extraReadProcessor;
    private final LCP<?> targetProp;
    private final boolean clientAction;
    private final boolean dialog;

    public ReadActionProperty(ValueClass sourceProp, LCP<?> targetProp, boolean clientAction, boolean dialog) {
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
        DataObject sourceProp = context.getDataKeys().getValue(0);
        assert sourceProp.getType() instanceof StringClass;
        String sourcePath = (String) sourceProp.object;

        try {

            boolean isDynamicFormatFileClass = targetProp.property.getType() instanceof DynamicFormatFileClass;
            boolean isBlockingFileRead = Settings.get().isBlockingFileRead();
            ReadUtils.ReadResult readResult;
            if (clientAction) {
                readResult = (ReadUtils.ReadResult) context.requestUserInteraction(new ReadClientAction(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, dialog));
                if (readResult != null) {
                    targetProp.change(readResult.fileBytes, context);
                }
            } else {
                readResult = ReadUtils.readFile(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, false, extraReadProcessor);
                if (readResult != null) {
                    if(isDynamicFormatFileClass)
                        targetProp.change((FileData)readResult.fileBytes, context);
                    else
                        targetProp.change((RawFileData)readResult.fileBytes, context);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(targetProp.property);
    }
}
