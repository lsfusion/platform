package platform.server.logics.property.actions.flow;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static platform.base.BaseUtils.reverse;

public class ListActionProperty extends KeepContextActionProperty {

    private final List<ActionPropertyMapImplement<ClassPropertyInterface>> actions;

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> ListActionProperty(String sID, String caption, List<I> innerInterfaces, List<ActionPropertyMapImplement<I>> actions, BusinessLogics BL)  {
        super(sID, caption, innerInterfaces, (List) actions);

        this.actions = DerivedProperty.mapActionImplements(reverse(getMapInterfaces(innerInterfaces)), actions);

        finalizeInit();
    }

    public Set<CalcProperty> getChangeProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(ActionPropertyMapImplement<ClassPropertyInterface> action : actions)
            result.addAll(action.property.getChangeProps());
        return result;
    }

    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(ActionPropertyMapImplement<ClassPropertyInterface> action : actions)
            result.addAll(action.property.getUsedProps());
        return result;
    }

    @Override
    public FlowResult execute(ExecutionContext context) throws SQLException {
        FlowResult result = FlowResult.FINISH;

        for (ActionPropertyMapImplement<ClassPropertyInterface> action : actions) {
            FlowResult actionResult = execute(context, action);
            if (actionResult != FlowResult.FINISH) {
                result =  actionResult;
                break;
            }
        }

        return result;
    }
}
