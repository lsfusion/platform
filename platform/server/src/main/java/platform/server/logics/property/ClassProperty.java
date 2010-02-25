package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.session.*;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;

import java.util.Collection;
import java.util.Map;

public class ClassProperty extends AggregateProperty<ClassPropertyInterface> {

    final ConcreteValueClass valueClass;
    final Object value;

    public ClassProperty(String sID, String caption, ValueClass[] classes, ConcreteValueClass valueClass, Object value) {
        super(sID, caption, DataProperty.getInterfaces(classes));
        
        this.valueClass = valueClass;
        this.value = value;

        assert value !=null;
    }

    public static <U extends Changes<U>> void modifyClasses(Collection<ClassPropertyInterface> interfaces, Modifier<U> modifier, U fill) {
        for(ClassPropertyInterface valueInterface : interfaces) {
            modifier.modifyAdd(fill, valueInterface.interfaceClass);
            modifier.modifyRemove(fill, valueInterface.interfaceClass);
        }
    }

    public <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        U result = modifier.newChanges();
        modifyClasses(interfaces, modifier, result);
        return result;
    }

    public static Where getIsClassWhere(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Where classWhere = Where.TRUE;
        for(Map.Entry<ClassPropertyInterface,? extends Expr> join : joinImplement.entrySet()) // берем (нужного класса and не remove'уты) or add'уты
            classWhere = classWhere.and(DataSession.getIsClassWhere(modifier.getSession(), join.getValue(),
                    join.getKey().interfaceClass, changedWhere));
        return classWhere;
    }

    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        // здесь session может быть null
        return new ValueExpr(value,valueClass).and(getIsClassWhere(joinImplement, modifier, changedWhere));
    }
}
