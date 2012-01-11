package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class DataProperty extends UserProperty {

    public ValueClass value;
    
    public DataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes);        
        this.value = value;
    }

    public static List<ClassPropertyInterface> getInterfaces(ValueClass[] classes) {
        List<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new ClassPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    public PropertyChanges calculateUsedChanges(PropertyChanges propChanges) {
        return PropertyChanges.EMPTY;
    }

    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        throw new RuntimeException("should not be"); // так как stored должен
    }

    public ValueClass getValueClass() {
        return value;
    }

    public void execute(ExecutionContext context) throws SQLException {
        context.getSession().changeProperty(this, context.getKeys(), context.getValue(), context.isGroupLast());
    }
}
