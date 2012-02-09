package platform.server.logics.property.actions.flow;

import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.List;

import static platform.base.BaseUtils.reverse;

public class ListActionProperty extends KeepContextActionProperty {

    private final List<PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface>> actions;

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> ListActionProperty(String sID, String caption, List<I> innerInterfaces, List<PropertyMapImplement<ClassPropertyInterface, I>> actions) {
        super(sID, caption, innerInterfaces, (List) actions);

        this.actions = DerivedProperty.mapImplements(reverse(getMapInterfaces(innerInterfaces)), actions);
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        for (PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> action : actions)
            execute(action, context);
    }
}
