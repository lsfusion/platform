package lsfusion.server.logics.service;

import lsfusion.server.caches.CacheStats;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class TurnCacheStatsOff extends ScriptingActionProperty {
    public TurnCacheStatsOff(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        CacheStats.readCacheStats = false;    
    }
}
