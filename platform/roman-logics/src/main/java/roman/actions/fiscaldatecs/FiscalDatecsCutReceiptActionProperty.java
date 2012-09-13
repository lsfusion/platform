package roman.actions.fiscaldatecs;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class FiscalDatecsCutReceiptActionProperty extends ScriptingActionProperty {

    public FiscalDatecsCutReceiptActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            if (context.requestUserInteraction(new FiscalDatecsCustomOperationClientAction(4)) == null)
                context.apply(LM.getBL());
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
