package platform.server.logics.property.actions.flow;

import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static platform.base.BaseUtils.*;

public class IfActionProperty extends KeepContextActionProperty {

    private final PropertyInterfaceImplement<ClassPropertyInterface> ifProp; // calculate
    private final PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> trueAction; // action
    private final PropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> falseAction; // action
    
    private final boolean ifClasses; // костыль из-за невозможности работы с ClassWhere, используется в UnionProperty для генерации editActions 

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, List<I> innerInterfaces, PropertyInterfaceImplement<I> ifProp, PropertyMapImplement<ClassPropertyInterface, I> trueAction, PropertyMapImplement<ClassPropertyInterface, I> falseAction, boolean ifClasses) {
        super(sID, caption, innerInterfaces, toListNoNull(ifProp, trueAction, falseAction));

        Map<I, ClassPropertyInterface> mapInterfaces = reverse(getMapInterfaces(innerInterfaces));
        this.ifProp = ifProp.map(mapInterfaces);
        this.trueAction = trueAction.map(mapInterfaces);
        this.falseAction = falseAction!=null?falseAction.map(mapInterfaces):null;
        this.ifClasses = ifClasses;

        finalizeInit();
    }

    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, List<I> innerInterfaces, PropertyInterfaceImplement<I> ifProp, PropertyMapImplement<ClassPropertyInterface, I> trueAction, boolean ifClasses) {
        this(sID, caption, innerInterfaces, ifProp, trueAction, null, ifClasses);
    }

    public Set<Property> getChangeProps() {
        Set<Property> result = ((ActionProperty) trueAction.property).getChangeProps();
        if(falseAction!=null)
            result = mergeSet(result, ((ActionProperty) falseAction.property).getChangeProps());
        return result;
    }

    public Set<Property> getUsedProps() {
        Set<Property> result = new HashSet<Property>(((ActionProperty) trueAction.property).getUsedProps());
        if(falseAction!=null)
            result.addAll(((ActionProperty) falseAction.property).getUsedProps());
        ifProp.mapFillDepends(result);
        return result;
    }

    @Override
    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        if (readIf(context)) {
            return execute(context, trueAction);
        } else if (falseAction != null) {
            return execute(context, falseAction);
        }
        return FlowResult.FINISH;
    }

    private boolean readIf(ExecutionContext context) throws SQLException {
        if(ifClasses)
            return new ClassWhere<ClassPropertyInterface>(DataObject.getMapClasses(context.getSession().getCurrentObjects(context.getKeys()))).
                    means(((PropertyMapImplement<?, ClassPropertyInterface>) ifProp).mapClassWhere());
        else
            return ifProp.read(context.getSession(), context.getKeys(), context.getModifier()) != null;
    }
}
