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

    private final boolean newSession;
    private final BusinessLogics BL;

    private final List<PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface>> actions;

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> ListActionProperty(String sID, String caption, List<I> innerInterfaces, List<PropertyMapImplement<ClassPropertyInterface, I>> actions, boolean newSession, boolean doApply, BusinessLogics BL) {
        super(sID, caption, innerInterfaces, (List) actions);

        this.BL = BL;
        this.newSession = newSession;

        if (newSession && doApply) {
            actions.add(new PropertyMapImplement<ClassPropertyInterface, I>(BL.LM.flowApply.property));
        }

        this.actions = DerivedProperty.mapImplements(reverse(getMapInterfaces(innerInterfaces)), actions);

        finalizeInit();
    }

    public Set<Property> getChangeProps() {
        Set<Property> result = new HashSet<Property>();
        for(PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> action : actions)
            result.addAll(((ActionProperty)action.property).getChangeProps());
        return result;
    }

    public Set<Property> getUsedProps() {
        Set<Property> result = new HashSet<Property>();
        for(PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> action : actions)
            result.addAll(((ActionProperty)action.property).getUsedProps());
        return result;
    }

    @Override
    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        ExecutionContext innerContext = newSession
                                        ? context.override(context.getSession().createSession())
                                        : context;

        FlowResult result = FlowResult.FINISH;

        for (PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> action : actions) {
            FlowResult actionResult = execute(innerContext, action);
            if (actionResult != FlowResult.FINISH) {
                result =  actionResult;
                break;
            }
        }

        if (newSession) {
            innerContext.getSession().close();

            context.addActions(innerContext.getActions());
            if (context.getFormInstance() != null) {
                context.getFormInstance().refreshData();
            }
        }
        return result;
    }
}
