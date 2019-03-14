package lsfusion.server.logics.classes.utils.geo;

import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.DataObject;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;

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
