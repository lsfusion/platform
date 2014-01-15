package lsfusion.server.logics.scripted;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: DAle
 * Date: 15.11.12
 * Time: 17:11
 */

public class EvalActionProperty<P extends PropertyInterface> extends SystemExplicitActionProperty {
    private final LCP<P> source;
    private final ImMap<P, ClassPropertyInterface> mapSource;
    private static AtomicLong counter = new AtomicLong(0);

    public EvalActionProperty(String sID, String caption, LCP<P> source) {
        super(sID, caption, source.getInterfaceClasses());
        mapSource = source.listInterfaces.mapSet(getOrderInterfaces());
        this.source = source;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

    private String getScript(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<P, ? extends ObjectValue> sourceToData = mapSource.join(context.getKeys());
        return (String) source.read(context, source.listInterfaces.mapOrder(sourceToData).toArray(new ObjectValue[interfaces.size()]));
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
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
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

            LAP<?> runAction = module.getLAPByOldName("run");
            if (runAction != null && errString.isEmpty()) {
                String textScript = (String) module.findLCPByCompoundOldName("scriptStorage").read(context);
                if (module.findLCPByCompoundOldName("countTextScript").read(context) == null) {
                    DataSession session = context.createSession();
                    DataObject scriptObject = session.addObject((ConcreteCustomClass) module.findClassByCompoundName("Script"));
                    module.findLCPByCompoundOldName("textScript").change(textScript, session, scriptObject);
                    module.findLCPByCompoundOldName("dateTimeScript").change(new Timestamp(Calendar.getInstance().getTime().getTime()), session, scriptObject);
                    session.apply(context);
                }
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
