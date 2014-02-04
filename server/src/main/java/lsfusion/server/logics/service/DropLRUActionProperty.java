package lsfusion.server.logics.service;

import lsfusion.base.col.lru.ALRUMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class DropLRUActionProperty extends ScriptingActionProperty {

    public DropLRUActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ALRUMap.forceRemoveAllLRU(1.0);
    }
}
