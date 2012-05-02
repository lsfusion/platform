package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.Collection;
import java.util.Map;

public abstract class ExecuteClassProperty extends ExecuteProperty {

    public ExecuteClassProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    protected QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return getInterfaceClassProperty().property.getUsedChanges(propChanges);
    }

    protected abstract Expr getValueExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement);
    
    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getValueExpr(joinImplement).and(getInterfaceClassProperty().mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere());
    }

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        return BaseUtils.add(super.calculateLinks(),
                new Pair<Property<?>, LinkType>(getInterfaceClassProperty().property, LinkType.USEDACTION));
    }
}
