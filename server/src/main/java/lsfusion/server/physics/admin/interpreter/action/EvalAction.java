package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.interpreter.EvalUtils;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.List;

public class EvalAction<P extends PropertyInterface> extends SystemAction {

    protected PropertyInterface sourceInterface;
    protected ImOrderSet<PropertyInterface> paramInterfaces;
    protected ImMap<PropertyInterface, Type> paramTypes;

    private boolean action;

    public EvalAction(ImList<Type> params, boolean action) {
        super(LocalizedString.NONAME, SetFact.toOrderExclSet(params.size() + 1, new GetIndex<PropertyInterface>() {
            @Override
            public PropertyInterface getMapValue(int i) {
                return new PropertyInterface();
            }
        }));
        
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
        return paramInterfaces.mapList(context.getKeys()).toArray(new ObjectValue[paramInterfaces.size()]);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String script = getScript(context);

        LA<?> runAction = context.getBL().evaluateRun(script, action);
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
