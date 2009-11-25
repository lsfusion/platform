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

public class ClassProperty extends AggregateProperty<DataPropertyInterface> {

    final ConcreteValueClass valueClass;
    final Object value;

    public ClassProperty(String sID, String caption, ValueClass[] classes, ConcreteValueClass valueClass, Object value) {
        super(sID, caption, DataProperty.getInterfaces(classes));
        
        this.valueClass = valueClass;
        this.value = value;

        assert value !=null;
    }

    public static <U extends DataChanges<U>> void modifyClasses(Collection<DataPropertyInterface> interfaces, Modifier<U> modifier, U fill) {
        for(DataPropertyInterface valueInterface : interfaces) {
            modifier.modifyAdd(fill, valueInterface.interfaceClass);
            modifier.modifyRemove(fill, valueInterface.interfaceClass);
        }
    }

    <U extends DataChanges<U>> U calculateUsedChanges(Modifier<U> modifier) {
        U result = modifier.newChanges();
        modifyClasses(interfaces, modifier, result);
        return result;
    }

    public static Where getIsClassWhere(Map<DataPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Where classWhere = Where.TRUE;
        for(Map.Entry<DataPropertyInterface,? extends Expr> join : joinImplement.entrySet()) // берем (нужного класса and не remove'уты) or add'уты
            classWhere = classWhere.and(DataSession.getIsClassWhere(modifier.getSession(), join.getValue(),
                    join.getKey().interfaceClass, changedWhere));
        return classWhere;
    }

    public Expr calculateExpr(Map<DataPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        // здесь session может быть null
        return new ValueExpr(value,valueClass).and(getIsClassWhere(joinImplement, modifier, changedWhere));
    }
}
