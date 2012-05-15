package platform.server.logics.property.actions.flow;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.PropertyChanges;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static platform.base.BaseUtils.reverse;
import static platform.base.BaseUtils.toListNoNull;

public class IfActionProperty extends KeepContextActionProperty {

    private final CalcPropertyInterfaceImplement<ClassPropertyInterface> ifProp;
    private final ActionPropertyMapImplement<ClassPropertyInterface> trueAction;
    private final ActionPropertyMapImplement<ClassPropertyInterface> falseAction;

    private final boolean ifClasses; // костыль из-за невозможности работы с ClassWhere, используется в UnionProperty для генерации editActions 

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, boolean not, List<I> innerInterfaces, CalcPropertyInterfaceImplement<I> ifProp, ActionPropertyMapImplement<I> trueAction, ActionPropertyMapImplement<I> falseAction, boolean ifClasses) {
        super(sID, caption, innerInterfaces, toListNoNull(ifProp, trueAction, falseAction));

        Map<I, ClassPropertyInterface> mapInterfaces = reverse(getMapInterfaces(innerInterfaces));
        this.ifProp = ifProp.map(mapInterfaces);
        ActionPropertyMapImplement<ClassPropertyInterface> mapTrue = trueAction.map(mapInterfaces);
        ActionPropertyMapImplement<ClassPropertyInterface> mapFalse = falseAction != null ? falseAction.map(mapInterfaces) : null;
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

    @Override
    protected Where calculateWhere(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Where actionsWhere = Where.FALSE;
        Expr ifExpr = ifProp.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
        if (trueAction != null) {
            Expr trueExpr = trueAction.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            actionsWhere = actionsWhere.or(ifExpr.getWhere().and(trueExpr.getWhere()));
        }
        if (falseAction != null) {
            Expr falseExpr = falseAction.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            actionsWhere = actionsWhere.or(ifExpr.getWhere().not().and(falseExpr.getWhere()));
        }

        return actionsWhere.and(
                super.calculateWhere(joinImplement, propClasses, propChanges, changedWhere)
        );
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
    public FlowResult execute(ExecutionContext context) throws SQLException {
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

    private boolean readIf(ExecutionContext context) throws SQLException {
        if (ifClasses) {
            return new ClassWhere<ClassPropertyInterface>(DataObject.getMapClasses(context.getSession().getCurrentObjects(context.getKeys()))).
                    means(((CalcPropertyMapImplement<?, ClassPropertyInterface>) ifProp).mapClassWhere());
        } else {
            return ifProp.read(context, context.getKeys()) != null;
        }
    }
}
