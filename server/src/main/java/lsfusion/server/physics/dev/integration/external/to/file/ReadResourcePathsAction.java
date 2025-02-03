package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.server.base.ResourceUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class ReadResourcePathsAction extends InternalAction {

    public ReadResourcePathsAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String pattern = (String) getParam(0, context);
        boolean fullPaths = getParam(1, context) != null;

        List<String> allResources = ResourceUtils.getResources(Pattern.compile(pattern), fullPaths);
        allResources
                .forEach(r -> {
                    try {
                        findProperty("resourcePaths[STRING]").change(r, context, new DataObject(r, StringClass.text));
                    } catch (ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
                        throw Throwables.propagate(e);
                    }
                });
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
