package lsfusion.server.logics.classes.data.utils.geo;

import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class GeoActionProperty extends ScriptingAction {

    public GeoActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    public GeoActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
    }

    protected boolean isYandex(ExecutionContext context, DataObject mapProvider) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        boolean isYandex = true;
        if (mapProvider != null) {
            String providerName = ((String) findProperty("staticName[Object]").read(context, mapProvider));
            isYandex = providerName == null || providerName.contains("yandex");
        }
        return isYandex;
    }
}
