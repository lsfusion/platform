package platform.server.logics.property.actions.flow;

import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.reverse;
import static platform.base.BaseUtils.toList;

public class IfActionProperty extends KeepContextActionProperty {

    private final PropertyInterfaceImplement<ClassPropertyInterface> ifProp; // calculate
    private final PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> trueAction; // action
    private final PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> falseAction; // action

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, List<I> innerInterfaces, PropertyInterfaceImplement<I> ifProp, PropertyMapImplement<ClassPropertyInterface, I> trueAction, PropertyMapImplement<ClassPropertyInterface, I> falseAction) {
        super(sID, caption, innerInterfaces, toList(ifProp, trueAction, falseAction));

        Map<I,ClassPropertyInterface> mapInterfaces = reverse(getMapInterfaces(innerInterfaces));
        this.ifProp = ifProp.map(mapInterfaces);
        this.trueAction = trueAction.map(mapInterfaces);
        this.falseAction = falseAction.map(mapInterfaces);
    }

    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, List<I> innerInterfaces, PropertyInterfaceImplement<I> ifProp, PropertyMapImplement<ClassPropertyInterface, I> trueAction) {
        super(sID, caption, innerInterfaces, toList(ifProp, trueAction));

        Map<I,ClassPropertyInterface> mapInterfaces = reverse(getMapInterfaces(innerInterfaces));
        this.ifProp = ifProp.map(mapInterfaces);
        this.trueAction = trueAction.map(mapInterfaces);
        this.falseAction = null;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        if(ifProp.read(context.getSession(), context.getKeys(), context.getModifier())!=null)
            execute(trueAction, context);
        else if (falseAction != null)
            execute(falseAction, context);
    }
}
