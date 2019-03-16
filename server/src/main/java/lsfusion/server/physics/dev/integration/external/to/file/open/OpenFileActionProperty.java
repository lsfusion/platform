package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class OpenFileActionProperty extends InternalAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface nameInterface;

    public OpenFileActionProperty(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
        nameInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            FileData source = (FileData) context.getKeyValue(sourceInterface).getValue();
            String name = (String) context.getKeyValue(nameInterface).getValue();

            if (source != null) {
                context.delayUserInteraction(new OpenFileClientAction(source.getRawFile(), name, source.getExtension()));
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}