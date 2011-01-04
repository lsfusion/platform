package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.classes.StaticClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.SimpleChanges;
import platform.server.logics.DataObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassProperty extends AggregateProperty<ClassPropertyInterface> {

    final StaticClass staticClass;
    final Object value;

    public ClassProperty(String sID, String caption, ValueClass[] classes, StaticClass staticClass, Object value) {
        super(sID, caption, DataProperty.getInterfaces(classes));
        
        this.staticClass = staticClass;
        this.value = value;

        assert value !=null;
    }

    public static Set<ValueClass> getValueClasses(Collection<ClassPropertyInterface> interfaces) {
        Set<ValueClass> interfaceClasses = new HashSet<ValueClass>();
        for(ClassPropertyInterface valueInterface : interfaces)
            interfaceClasses.add(valueInterface.interfaceClass);
        return interfaceClasses;
    }

    public static <U extends Changes<U>> U getIsClassUsedChanges(Collection<ClassPropertyInterface> interfaces, Modifier<U> modifier) {
        return modifier.newChanges().addChanges(new SimpleChanges(modifier.getChanges(), getValueClasses(interfaces), false));
    }

    public <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return getIsClassUsedChanges(interfaces, modifier);
    }

    public static Where getIsClassWhere(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Where classWhere = Where.TRUE;
        for(Map.Entry<ClassPropertyInterface,? extends Expr> join : joinImplement.entrySet()) // берем (нужного класса and не remove'уты) or add'уты
            classWhere = classWhere.and(modifier.getSession().getIsClassWhere(join.getValue(), join.getKey().interfaceClass, changedWhere));
        return classWhere;
    }

    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        // здесь session может быть null
        return staticClass.getStaticExpr(value).and(getIsClassWhere(joinImplement, modifier, changedWhere));
    }
}
