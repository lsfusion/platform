package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class TestActionActionProperty extends ScriptingActionProperty {

    public TestActionActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            
            testAction();

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    protected void testAction() throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        long j = 0;
        for(long z = 0; z < 1000000000; z++) {
            for (long i = 0; i < 1000000000; i++) {
                j++;
            }
        }
        System.out.println(j);
    }

}