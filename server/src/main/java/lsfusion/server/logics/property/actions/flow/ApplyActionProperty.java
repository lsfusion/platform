package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class ApplyActionProperty extends KeepContextActionProperty {
    private final ActionPropertyMapImplement<?, PropertyInterface> action;
    private final CalcProperty canceled;

    public <I extends PropertyInterface> ApplyActionProperty(BaseLogicsModule LM, ActionPropertyMapImplement<?, I> action,
                                                             String sID, String caption, ImOrderSet<I> innerInterfaces) {
        super(sID, caption, innerInterfaces.size());

        this.action = (ActionPropertyMapImplement<?, PropertyInterface>) action;
        this.canceled = LM.getLCPByOldName("canceled").property;
        
        finalizeInit();
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

        FlowResult result = action.execute(context);
        if (context.apply())
            return result;
        else {
            if(canceled != null)
                canceled.change(context, true);
            return FlowResult.THROWS;
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
