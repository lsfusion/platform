package platform.server.logics.property.actions.flow;

import platform.server.caches.IdentityLazy;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.reverse;

public class ListActionProperty extends KeepContextActionProperty {

    private final List<ActionPropertyMapImplement<?, PropertyInterface>> actions;

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> ListActionProperty(String sID, String caption, List<I> innerInterfaces, List<ActionPropertyMapImplement<?, I>> actions, BusinessLogics BL)  {
        super(sID, caption, innerInterfaces.size());

        this.actions = DerivedProperty.mapActionImplements(reverse(getMapInterfaces(innerInterfaces)), actions);

        finalizeInit();
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        List<CalcPropertyMapImplement<?, PropertyInterface>> listWheres = new ArrayList<CalcPropertyMapImplement<?, PropertyInterface>>();
        for(ActionPropertyMapImplement<?, PropertyInterface> action : actions)
            listWheres.add(action.mapWhereProperty());
        return DerivedProperty.createUnion(interfaces, listWheres);
    }

    public Set<CalcProperty> getChangeProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(ActionPropertyMapImplement<?, PropertyInterface> action : actions)
            result.addAll(action.property.getChangeProps());
        return result;
    }

    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(ActionPropertyMapImplement<?, PropertyInterface> action : actions)
            result.addAll(action.property.getUsedProps());
        return result;
    }

    @Override
    public FlowResult execute(ExecutionContext<PropertyInterface> context) throws SQLException {
        FlowResult result = FlowResult.FINISH;

        for (ActionPropertyMapImplement<?, PropertyInterface> action : actions) {
            FlowResult actionResult = execute(context, action);
            if (actionResult != FlowResult.FINISH) {
                result =  actionResult;
                break;
            }
        }

        return result;
    }
}
