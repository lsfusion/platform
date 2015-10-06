package lsfusion.server.logics.service;

import lsfusion.base.col.lru.ALRUMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class DropLRUActionProperty extends ScriptingActionProperty {

    public DropLRUActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ALRUMap.forceRemoveAllLRU(((Double) context.getSingleDataKeyValue().object)/100.0);
    }
}
