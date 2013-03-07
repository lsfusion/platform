package platform.server.logics.scripted;

import com.google.common.base.Throwables;
import org.antlr.runtime.RecognitionException;
import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.MessageClientAction;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: DAle
 * Date: 15.11.12
 * Time: 17:11
 */

public class EvalActionProperty<P extends PropertyInterface> extends SystemActionProperty {
    private final LCP<P> source;
    private final ImMap<P, ClassPropertyInterface> mapSource;
    private static AtomicLong counter = new AtomicLong(0);

    public EvalActionProperty(String sID, String caption, LCP<P> source) {
        super(sID, caption, source.getInterfaceClasses());
        mapSource = source.listInterfaces.mapSet(getOrderInterfaces());
        this.source = source;
    }

    private String getScript(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        ImMap<P, DataObject> sourceToData = mapSource.join(context.getKeys());
        return (String) source.read(context, source.listInterfaces.mapOrder(sourceToData).toArray(new DataObject[interfaces.size()]));
    }

    private String getUniqueName() {
        return "UNIQUE" + counter.incrementAndGet() + "NSNAME";
    }

    private String wrapScript(BusinessLogics<?> BL, String script) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("MODULE ");
        strBuilder.append(getUniqueName());
        strBuilder.append("; ");
        strBuilder.append("REQUIRE ");
        boolean isFirst = true;
        for (LogicsModule module : BL.getLogicModules()) {
            if (!isFirst) {
                strBuilder.append(", ");
            }
            isFirst = false;
            strBuilder.append(module.getName());
        }
        strBuilder.append("; ");
        strBuilder.append(script);
        return strBuilder.toString();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        BusinessLogics BL = context.getBL();

        String script = getScript(context);
        ScriptingLogicsModule module = new ScriptingLogicsModule(BL.LM, BL, wrapScript(BL, script));
        String errString = "";
        try {
            module.initModuleDependencies();
            module.initModule();
            module.initAliases();
            module.initProperties();

            errString = module.getErrorsDescription();

            LAP<?> runAction = module.getLAPByName("run");
            if (runAction != null && errString.isEmpty()) {
                runAction.execute(context);
            }
        } catch (RecognitionException e) {
            errString = module.getErrorsDescription() + e.getMessage();
        } catch (Exception e) {
            if (!module.getErrorsDescription().isEmpty()) {
                errString = module.getErrorsDescription() + e.getMessage();
            } else {
                Throwables.propagate(e);
            }
        }

        if (!errString.isEmpty()) {
            context.requestUserInteraction(new MessageClientAction(errString, "parse error"));
        }
    }
}
