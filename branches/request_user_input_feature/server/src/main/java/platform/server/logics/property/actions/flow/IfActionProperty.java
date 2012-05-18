package platform.server.logics.property.actions.flow;

import platform.server.caches.IdentityLazy;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static platform.base.BaseUtils.reverse;

public class IfActionProperty extends KeepContextActionProperty {

    private final CalcPropertyInterfaceImplement<PropertyInterface> ifProp;
    private final ActionPropertyMapImplement<?, PropertyInterface> trueAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> falseAction;

    private final boolean ifClasses; // костыль из-за невозможности работы с ClassWhere на уровне свойств, используется в UnionProperty для генерации editActions

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, boolean not, List<I> innerInterfaces, CalcPropertyInterfaceImplement<I> ifProp, ActionPropertyMapImplement<?, I> trueAction, ActionPropertyMapImplement<?, I> falseAction, boolean ifClasses) {
        super(sID, caption, innerInterfaces.size());

        Map<I, PropertyInterface> mapInterfaces = reverse(getMapInterfaces(innerInterfaces));
        this.ifProp = ifProp.map(mapInterfaces);
        ActionPropertyMapImplement<?, PropertyInterface> mapTrue = trueAction.map(mapInterfaces);
        ActionPropertyMapImplement<?, PropertyInterface> mapFalse = falseAction != null ? falseAction.map(mapInterfaces) : null;
        if (!not) {
            this.trueAction = mapTrue;
            this.falseAction = mapFalse;
        } else {
            this.trueAction = mapFalse;
            this.falseAction = mapTrue;
        }

        this.ifClasses = ifClasses;

        finalizeInit();
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createIfElseUProp(interfaces, ifProp,
                trueAction != null ? trueAction.mapWhereProperty() : null,
                falseAction !=null ? falseAction.mapWhereProperty() : null, ifClasses);
    }

    public Set<CalcProperty> getChangeProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        if (trueAction != null) {
            result.addAll(trueAction.property.getChangeProps());
        }
        if (falseAction != null) {
            result.addAll(falseAction.property.getChangeProps());
        }
        return result;
    }

    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        if (trueAction != null) {
            result.addAll(new HashSet<CalcProperty>(trueAction.property.getUsedProps()));
        }
        if (falseAction != null) {
            result.addAll(falseAction.property.getUsedProps());
        }
        ifProp.mapFillDepends(result);
        return result;
    }

    @Override
    public FlowResult execute(ExecutionContext<PropertyInterface> context) throws SQLException {
        if (readIf(context)) {
            if (trueAction != null) {
                return execute(context, trueAction);
            }
        } else {
            if (falseAction != null) {
                return execute(context, falseAction);
            }
        }
        return FlowResult.FINISH;
    }

    private boolean readIf(ExecutionContext<PropertyInterface> context) throws SQLException {
        if (ifClasses) {
            return new ClassWhere<PropertyInterface>(DataObject.getMapClasses(context.getSession().getCurrentObjects(context.getKeys()))).
                    means(((CalcPropertyMapImplement<?, PropertyInterface>) ifProp).mapClassWhere());
        } else {
            return ifProp.read(context, context.getKeys()) != null;
        }
    }
}
