package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.ReadSourceType;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;
import java.sql.SQLException;

public class ReadActionProperty extends ScriptingActionProperty {
    private final ReadSourceType type;
    private final LCP<?> targetProp;

    public ReadActionProperty(ScriptingLogicsModule LM, ReadSourceType type, ValueClass valueClass, LCP<?> targetProp) {
        super(LM, valueClass);
        this.type = type;
        this.targetProp = targetProp;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof StringClass;

        String path = (String) value.object;
        try {
            if (type == ReadSourceType.FILE) {
                File file = new File(path);
                if (file.exists()) {
                    targetProp.change(IOUtils.getFileBytes(file), context);
                }
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
