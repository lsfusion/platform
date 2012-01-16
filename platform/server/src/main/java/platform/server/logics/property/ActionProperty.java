package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.classes.ActionClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.Map;

public abstract class ActionProperty extends ExecuteProperty {

    public ActionProperty(String sID, ValueClass... classes) {
        this(sID, "sysAction", classes);
    }

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    @Override
    public void prereadCaches() {
        super.prereadCaches();
        ClassProperty.getIsClassProperty(interfaces).property.prereadCaches();
    }

    protected QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return ClassProperty.getIsClassUsed(interfaces, propChanges);
    }
    
    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getValueClass().getDefaultExpr().and(ClassProperty.getIsClassWhere(joinImplement, propChanges, changedWhere));
    }

    public DataClass getValueClass() {
        return ActionClass.instance;
    }
}
