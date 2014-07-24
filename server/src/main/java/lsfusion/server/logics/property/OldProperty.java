package lsfusion.server.logics.property;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.OldDrillDownFormEntity;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.session.PropertyChanges;

import static lsfusion.base.BaseUtils.capitalize;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class OldProperty<T extends PropertyInterface> extends SessionCalcProperty<T> {

    public final PrevScope scope;

    public OldProperty(CalcProperty<T> property, PrevScope scope) {
        super(property.caption + " (в БД)", property);

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
            return getClassTableExpr(joinImplement, calcType);

        return property.getExpr(joinImplement); // возвращаем старое значение
    }

    @Override
    public ClassWhere<Object> getClassValueWhere(ClassType type, PrevClasses prevSameClasses) {
        ClassWhere<Object> classValueWhere = property.getClassValueWhere(ClassType.ASSERTFULL, prevSameClasses);
        switch (prevSameClasses) {
            case SAME:
                return classValueWhere;
            case INVERTSAME:
                return classValueWhere;
            case BASE:
                return classValueWhere.getBase();
        }
        throw new UnsupportedOperationException();    
    }

    public ImMap<T, ValueClass> getInterfaceCommonClasses(ValueClass commonValue, PrevClasses prevSameClasses) {
        return property.getInterfaceCommonClasses(commonValue, prevSameClasses);
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
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new OldDrillDownFormEntity(
                "drillDown" + capitalize(getUniqueSID()) + "Form",
                getString("logics.property.drilldown.form.old"), this, LM
        );
    }

    @Override
    public ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events) {
        if(Settings.get().isUseEventValuePrevHeuristic() && property instanceof AggregateProperty && ((AggregateProperty)property).hasAlotKeys())
            return SetFact.EMPTY();
        return super.getSessionCalcDepends(events);
    }
}
