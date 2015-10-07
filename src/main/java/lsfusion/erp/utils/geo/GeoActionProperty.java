package lsfusion.erp.utils.geo;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class GeoActionProperty extends ScriptingActionProperty {

    public GeoActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public GeoActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
    }

    protected boolean isYandex(ExecutionContext context, DataObject mapProvider) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        boolean isYandex = true;
        if (mapProvider != null) {
            String providerName = ((String) findProperty("staticName").read(context, mapProvider));
            isYandex = providerName == null || providerName.contains("yandex");
        }
        return isYandex;
    }
}
