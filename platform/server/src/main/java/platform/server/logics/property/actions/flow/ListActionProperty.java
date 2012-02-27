package platform.server.logics.property.actions.flow;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyMapImplement;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.List;

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
            actions.add(new PropertyMapImplement<ClassPropertyInterface, I>(BL.LM.apply.property));
        }

        this.actions = DerivedProperty.mapImplements(reverse(getMapInterfaces(innerInterfaces)), actions);
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        ExecutionContext innerContext = newSession
                                        ? context.override(context.getSession().createSession())
                                        : context;

        for (PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> action : actions) {
            execute(action, innerContext);
        }

        if (newSession) {
            innerContext.getSession().close();

            context.addActions(innerContext.getActions());
            if (context.getFormInstance() != null) {
                context.getFormInstance().refreshData();
            }
        }
    }
}
