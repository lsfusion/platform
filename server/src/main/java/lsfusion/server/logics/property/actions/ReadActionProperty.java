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
    private final boolean delete;

    public ReadActionProperty(ValueClass sourceProp, LCP<?> targetProp, ValueClass moveProp, boolean clientAction, boolean dialog, boolean delete) {
        super(moveProp == null ? new ValueClass[]{sourceProp} : new ValueClass[]{sourceProp, moveProp});
        this.targetProp = targetProp;
        this.clientAction = clientAction;
        this.dialog = dialog;
        this.delete = delete;

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Class.forName("com.informix.jdbc.IfxDriver");
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

        String movePath = null;
        if (context.getDataKeys().size() == 2) {
            DataObject moveProp = context.getDataKeys().getValue(1);
            assert moveProp.getType() instanceof StringClass;
            movePath = (String) moveProp.object;
        }

        try {

            boolean isDynamicFormatFileClass = targetProp.property.getType() instanceof DynamicFormatFileClass;
            boolean isBlockingFileRead = Settings.get().isBlockingFileRead();
            ReadUtils.ReadResult readResult;
            if (clientAction) {
                readResult = (ReadUtils.ReadResult) context.requestUserInteraction(new ReadClientAction(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, dialog));
                if (readResult.errorCode == 0) {
                    targetProp.change(readResult.fileBytes, context);
                    context.requestUserInteraction(new PostReadClientAction(sourcePath, readResult.type, readResult.filePath, movePath, delete));
                }
            } else {
                readResult = ReadUtils.readFile(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, false);
                if (readResult.errorCode == 0) {
                    targetProp.change(readResult.fileBytes, context);
                    ReadUtils.postProcessFile(sourcePath, readResult.type, readResult.filePath, movePath, delete);
                }

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
