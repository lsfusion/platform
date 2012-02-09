package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.classes.ActionClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ActionProperty extends ExecuteClassProperty {

    public ActionProperty(String sID, ValueClass... classes) {
        this(sID, "sysAction", classes);
    }

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    protected Expr getValueExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement) {
        return getValueClass().getDefaultExpr();
    }

    public DataClass getValueClass() {
        return ActionClass.instance;
    }

    protected static ValueClass or(ValueClass v1, ValueClass v2) {
        if(v1==null)
            return v2;
        if(v2==null)
            return v1;
        return v1.getUpSet().getOr().or(v2.getUpSet().getOr()).getCommonClass();
    }

    protected static <I extends PropertyInterface> ValueClass[] getClasses(List<I> mapInterfaces, Collection<PropertyInterfaceImplement<I>> props) {
        ValueClass[] result = new ValueClass[mapInterfaces.size()];
        for(PropertyInterfaceImplement<I> prop : props) {
            Map<I, ValueClass> propClasses;
            if(prop instanceof PropertyMapImplement)
                propClasses = ((PropertyMapImplement<?, I>) prop).mapCommonInterfaces();
            else
                propClasses = new HashMap<I, ValueClass>();

            for(int i=0;i<result.length;i++)
                result[i] = or(result[i], propClasses.get(mapInterfaces.get(i)));
        }
        return result;
    }

}
