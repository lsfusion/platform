package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.OldDrillDownFormEntity;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.session.PropertyChanges;

import static lsfusion.base.BaseUtils.capitalize;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class OldProperty<T extends PropertyInterface> extends SessionCalcProperty<T> {

    public final PrevScope scope;

    public OldProperty(CalcProperty<T> property, PrevScope scope) {
        super("OLD_" + property.getSID() + "_" + scope.getSID(), property.caption + " (в БД)", property);

        this.scope = scope;
    }

/*    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        return BaseUtils.add(super.calculateLinks(), new Pair<Property<?>, LinkType>(property, LinkType.EVENTACTION)); // чтобы лексикографику для applied была
    }*/

    public OldProperty<T> getOldProperty() {
        return this;
    }

    protected Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(calcType.isClass())
            return getClassTableExpr(joinImplement);

        return property.getExpr(joinImplement); // возвращаем старое значение
    }

    @Override
    public ClassWhere<Object> getClassValueWhere(ClassType type) {
        return property.getClassValueWhere(ClassType.ASSERTFULL);
    }

    public ImMap<T, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        return property.getInterfaceCommonClasses(commonValue);
    }

    @Override
    public boolean supportsDrillDown() {
        return property != null;
    }

    @Override
    public boolean drillDownInNewSession() {
        return true;
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new OldDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.old"), this, BL
        );
    }
}
