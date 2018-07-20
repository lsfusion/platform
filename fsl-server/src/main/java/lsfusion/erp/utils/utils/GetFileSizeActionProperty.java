package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class GetFileSizeActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface fileInterface;

    public GetFileSizeActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            findProperty("fileSize[]").change(BaseUtils.getFile((byte[]) context.getDataKeyValue(fileInterface).object).length, context);
        } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }
}