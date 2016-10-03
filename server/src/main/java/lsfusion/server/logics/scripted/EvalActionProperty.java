package lsfusion.server.logics.scripted;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * User: DAle
 * Date: 15.11.12
 * Time: 17:11
 */

public class EvalActionProperty<P extends PropertyInterface> extends SystemExplicitActionProperty {
    private final LCP<P> source;
    private final ImMap<P, ClassPropertyInterface> mapSource;

    public EvalActionProperty(LocalizedString caption, LCP<P> source) {
        super(caption, source.getInterfaceClasses(ClassType.aroundPolicy));
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

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        BusinessLogics BL = context.getBL();

        ScriptingLogicsModule evalLM = BL.getModule("EvalScript");

        String script = getScript(context);

        try {
            ScriptingLogicsModule module = EvalUtils.evaluate(context.getBL(), script);

            String runName = module.getName() + ".run";
            LAP<?> runAction = module.findAction(runName);
            if (runAction != null) {
                String textScript = (String) evalLM.findProperty("scriptStorage[]").read(context);
                try (DataSession session = context.createSession()) {
                    ObjectValue scriptObject = evalLM.findProperty("textScript[]").readClasses(session);
                    if (scriptObject instanceof NullValue) {
                        scriptObject = session.addObject((ConcreteCustomClass) evalLM.findClass("Script"));
                        evalLM.findProperty("text[Script]").change(textScript, session, (DataObject) scriptObject);
                    }
                    evalLM.findProperty("dateTime[Script]").change(new Timestamp(Calendar.getInstance().getTime().getTime()), session, (DataObject) scriptObject);
                    session.apply(context);
                }
                runAction.execute(context);
            }
        } catch (EvalUtils.EvaluationException | RecognitionException e) {
            context.delayUserInteraction(new MessageClientAction(e.getMessage(), "Parse error"));
        }
    }
}
