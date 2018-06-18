package lsfusion.server.logics.scripted;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.List;

public class EvalActionProperty<P extends PropertyInterface> extends SystemExplicitActionProperty {
    private final LCP<P> source;
    private final List<LCP<P>> params;
    private final ImMap<P, ClassPropertyInterface> mapSource;
    private boolean action;

    public EvalActionProperty(LocalizedString caption, LCP<P> source, List<LCP<P>> params, boolean action) {
        super(caption, source.getInterfaceClasses(ClassType.aroundPolicy));
        mapSource = source.listInterfaces.mapSet(getOrderInterfaces());
        this.source = source;
        this.params = params;
        this.action = action;
    }

    private String getScript(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<P, ? extends ObjectValue> sourceToData = mapSource.join(context.getKeys());
        return (String) source.read(context, source.listInterfaces.mapOrder(sourceToData).toArray(new ObjectValue[interfaces.size()]));
    }

    private ObjectValue[] getParams(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue[] result = new ObjectValue[params.size()];
        for(int i = 0; i < params.size(); i++) {
            LCP<P> param = params.get(i);
            ImMap<P, ? extends ObjectValue> paramToData = param.listInterfaces.mapSet(getOrderInterfaces()).join(context.getKeys());
            result[i] = param.readClasses(context, (DataObject[]) param.listInterfaces.mapOrder(paramToData).toArray(new DataObject[param.listInterfaces.size()]));
        }
        return result;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String script = getScript(context);

        try {
            LAP<?> runAction = context.getBL().evaluateRun(script, action);
            if (runAction != null)
                runAction.execute(context, getParams(context));
        } catch (EvalUtils.EvaluationException | RecognitionException e) {
            context.delayUserInteraction(new MessageClientAction(getMessage(e), "Parse error"));
            throw new RuntimeException(e);
        } catch (Throwable e) {
            context.delayUserInteraction(new MessageClientAction(getMessage(e), "Execution error"));
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }

    private String getMessage(Throwable e) {
        return e.getMessage() == null ? String.valueOf(e) : e.getMessage();
    }
}
