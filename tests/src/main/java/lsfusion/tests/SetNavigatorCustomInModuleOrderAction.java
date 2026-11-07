package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.lang.reflect.Field;
import java.sql.SQLException;

// two modules that do not require each other giving the navigator its CUSTOM, the later one in the module order writing
// first, as the parse threads' timing can have it. The server's own CUSTOM is final once started, so the test swaps it
// for a fresh NF property, reached by reflection, and puts the original back. What the test saw goes into ncFailure
public class SetNavigatorCustomInModuleOrderAction extends AtOnceAction {

    public SetNavigatorCustomInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LogicsModule[] modules = independentModules(context.getBL(), LM);
            BaseLogicsModule baseLM = context.getBL().LM;
            Field field = BaseLogicsModule.class.getDeclaredField("navigatorCustom");
            field.setAccessible(true);
            Object original = field.get(baseLM);

            String failure = null;
            field.set(baseLM, NFFact.property());
            try {
                baseLM.setNavigatorCustom("NcLater", modules[1].getVersion());
                baseLM.setNavigatorCustom("NcEarlier", modules[0].getVersion());
                String custom = baseLM.getNavigatorCustom();
                if (!"NcLater".equals(custom))
                    failure = "the navigator kept the CUSTOM " + custom + " of the module earlier in the module order ("
                            + modules[0].getName() + "), written last";
            } finally {
                field.set(baseLM, original);
            }

            findProperty("ncFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
