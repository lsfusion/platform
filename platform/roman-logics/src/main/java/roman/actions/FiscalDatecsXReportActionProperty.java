package roman.actions;

import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.classes.StaticCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
