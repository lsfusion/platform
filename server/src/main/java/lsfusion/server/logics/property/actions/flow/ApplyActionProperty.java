package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class ApplyActionProperty extends KeepContextActionProperty {
    private final ActionPropertyMapImplement<?, PropertyInterface> action;
    private final CalcProperty canceled;
    private final boolean keepAllSessionProperties;
    private final ImSet<SessionDataProperty> keepSessionProperties;

    public <I extends PropertyInterface> ApplyActionProperty(BaseLogicsModule LM, ActionPropertyMapImplement<?, I> action,
                                                             String sID, String caption, ImOrderSet<I> innerInterfaces,
                                                             boolean keepAllSessionProperties, ImSet<SessionDataProperty> keepSessionProperties) {
        super(sID, caption, innerInterfaces.size());
        this.keepAllSessionProperties = keepAllSessionProperties;
        this.keepSessionProperties = keepSessionProperties;

        this.action = action.map(getMapInterfaces(innerInterfaces).reverse());
        this.canceled = getCanceled(LM).property;
        
        finalizeInit();
    }
    
    private LCP<?> getCanceled(BaseLogicsModule lm) {
        try {
            return lm.findProperty("canceled");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return super.aspectUsedExtProps().replaceValues(true);
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        
        MList<ActionPropertyMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        if(action != null)
            actions.add(action);

        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listWheres =
                ((ImList<ActionPropertyMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        new GetValue<CalcPropertyInterfaceImplement<PropertyInterface>, ActionPropertyMapImplement<?, PropertyInterface>>() {
                            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(ActionPropertyMapImplement<?, PropertyInterface> value) {
                                return value.mapWhereProperty();
                            }});
        return DerivedProperty.createUnion(interfaces, listWheres);
        
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImSet<SessionDataProperty> keepProperties = getKeepProperties(context.getSession());

        if (!context.apply(action == null ? null : action.getValueImplement(context.getKeys()), keepProperties))
            if(canceled != null)
                canceled.change(context, true);

        return FlowResult.FINISH;
    }

    private ImSet<SessionDataProperty> getKeepProperties(DataSession session) throws SQLException, SQLHandledException {
        if (keepAllSessionProperties) {
            MExclSet<SessionDataProperty> mProps = SetFact.mExclSet();
            for (SessionDataProperty prop : session.getSessionDataChanges().keySet()) {
                mProps.exclAdd(prop);
            }
            return mProps.immutable();
        } else {
            return keepSessionProperties;
        }
    }

    public ImSet<ActionProperty> getDependActions() {
        ImSet<ActionProperty> result = SetFact.EMPTY();
        if (action != null) {
            result = result.merge(action.property);
        }        
        return result;
    }
}
