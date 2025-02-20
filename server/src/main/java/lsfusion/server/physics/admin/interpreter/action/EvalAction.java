package lsfusion.server.physics.admin.interpreter.action;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.EvalScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.controller.stack.SameThreadExecutionStack;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class EvalAction<P extends PropertyInterface> extends SystemAction {

    protected PropertyInterface sourceInterface;
    protected ImOrderSet<PropertyInterface> paramInterfaces;
    protected ImMap<PropertyInterface, Type> paramTypes;

    private boolean action;

    public EvalAction(ImList<Type> params, boolean action) {
        super(LocalizedString.NONAME, SetFact.toOrderExclSet(params.size() + 1, i -> new PropertyInterface()));

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

    private class EvalStack extends SameThreadExecutionStack {

        private final EvalScriptingLogicsModule evalLM;
        private final DataSession session;

        public EvalStack(EvalScriptingLogicsModule evalLM, DataSession session, ExecutionStack upStack) {
            super(upStack);

            this.evalLM = evalLM;
            this.session = session;
        }

        @Override
        protected DataSession getSession() {
            return session;
        }

        @Override
        public EvalScriptingLogicsModule getEvalLM() {
            return evalLM;
        }
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String script = getScript(context);

        ExecutionStack stack = context.stack;
        Pair<LA, EvalScriptingLogicsModule> evalResult = context.getBL().LM.evaluateRun(script, stack.getEvalLM(), action);
        return evalResult.first.execute(context.override(new EvalStack(evalResult.second, context.getSession(), stack)), getParams(context));
    }
    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type == ChangeFlowType.INTERACTIVEWAIT)
            return false; // in the executeExternal usage it's better to be optimistic
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
