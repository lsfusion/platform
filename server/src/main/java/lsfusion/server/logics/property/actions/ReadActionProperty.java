package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ReadActionProperty extends SystemExplicitActionProperty {
    private final LCP<?> targetProp;
    private final boolean clientAction;
    private final boolean dialog;

    public ReadActionProperty(ValueClass sourceProp, LCP<?> targetProp, boolean clientAction, boolean dialog) {
        super(sourceProp);
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
                readResult = ReadUtils.readFile(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, false);
            }
            if (readResult.errorCode == 0) {
                targetProp.change(readResult.fileBytes, context);
            }
            if(readResult.error != null)
                throw Throwables.propagate(new RuntimeException(readResult.error));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(targetProp.property);
    }
}
