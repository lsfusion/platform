package roman.actions.fiscaldatecs;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class FiscalDatecsXReportActionProperty extends ScriptingActionProperty {

    public FiscalDatecsXReportActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            if (context.checkApply(LM.getBL()))
                if(context.requestUserInteraction(new FiscalDatecsCustomOperationClientAction(1))==null)
                    context.apply(LM.getBL());
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
