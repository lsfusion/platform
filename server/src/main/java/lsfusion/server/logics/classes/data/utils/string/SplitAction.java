package lsfusion.server.logics.classes.data.utils.string;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class SplitAction extends InternalAction {
    private final ClassPropertyInterface stringInterface;
    private final ClassPropertyInterface delimiterInterface;

    public SplitAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        stringInterface = i.next();
        delimiterInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String value = (String) context.getDataKeyValue(stringInterface).getValue();
        String delimiter = (String) context.getDataKeyValue(delimiterInterface).getValue();

        try {
            LP splittedProp = findProperty("splitted[INTEGER]");
            context.getSession().dropChanges((DataProperty) splittedProp.property);
            String[] splitted = value.split(delimiter);
            for(int i = 0; i < splitted.length; i++) {
                splittedProp.change(splitted[i], context, new DataObject(i));
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
