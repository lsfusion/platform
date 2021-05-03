package lsfusion.server.physics.admin.interpreter.action;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class EvalAction<P extends PropertyInterface> extends SystemAction {

    private ScriptingLogicsModule LM;
    protected PropertyInterface sourceInterface;
    protected ImOrderSet<PropertyInterface> paramInterfaces;
    protected ImMap<PropertyInterface, Type> paramTypes;

    private boolean action;

    public EvalAction(ScriptingLogicsModule LM, ImList<Type> params, boolean action) {
        super(LocalizedString.NONAME, SetFact.toOrderExclSet(params.size() + 1, i -> new PropertyInterface()));

        this.LM = LM;
        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        sourceInterface = orderInterfaces.get(0);
        this.paramInterfaces = orderInterfaces.subOrder(1, orderInterfaces.size());
        paramTypes = paramInterfaces.mapList(params);
        this.action = action;
    }

    private String getScript(ExecutionContext<PropertyInterface> context) {
        return (String)context.getKeyObject(sourceInterface);
    }

    private ObjectValue[] getParams(ExecutionContext<PropertyInterface> context) {
        return paramInterfaces.<ObjectValue>mapList(context.getKeys()).toArray(new ObjectValue[paramInterfaces.size()]);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String script = getScript(context);

        LA<?> runAction = LM.evaluateRun(script, action);
        if (runAction != null)
            return runAction.execute(context, getParams(context));
        
        return FlowResult.FINISH;
    }
    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // execute just like EXEC operator
    }

    private String getMessage(Throwable e) {
        return e.getMessage() == null ? String.valueOf(e) : e.getMessage();
    }
}
