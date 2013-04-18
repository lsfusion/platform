package paas.properties;

import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class RefreshStatusActionProperty extends ScriptingActionProperty {

    public RefreshStatusActionProperty(PaasLogicsModule paasLM) {
        super(paasLM, paasLM.project);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        ((PaasBusinessLogics)context.getBL()).refreshConfigurationStatuses(context.getSingleKeyValue());

        context.emitExceptionIfNotInFormSession();

        context.getFormInstance().refreshData();
    }
}
