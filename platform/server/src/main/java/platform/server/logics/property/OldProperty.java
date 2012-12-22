package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.PropertyChanges;

public class OldProperty<T extends PropertyInterface> extends SessionCalcProperty<T> {

    public OldProperty(CalcProperty<T> property) {
        super("OLD_" + property.getSID(), property.caption + " (в БД)", property);
    }

/*    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        return BaseUtils.add(super.calculateLinks(), new Pair<Property<?>, LinkType>(property, LinkType.EVENTACTION)); // чтобы лексикографику для applied была
    }*/

    public OldProperty<T> getOldProperty() {
        return this;
    }

    protected Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses)
            return getClassTableExpr(joinImplement);

        return property.getExpr(joinImplement); // возвращаем старое значение
    }

    @Override
    public ClassWhere<Object> getClassValueWhere() {
        return property.getClassValueWhere();
    }

    public ImMap<T, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        return property.getInterfaceCommonClasses(commonValue);
    }
}
