package platform.server.logics.properties;

import platform.server.data.classes.ConcreteValueClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.session.DataChanges;
import platform.server.session.DataSession;
import platform.server.session.TableChanges;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.Map;

public class ClassProperty extends AggregateProperty<DataPropertyInterface> {

    final ConcreteValueClass valueClass;
    final Object value;

    public ClassProperty(String iSID, ValueClass[] classes, ConcreteValueClass iValueClass, Object iValue) {
        super(iSID, DataProperty.getInterfaces(classes));
        
        valueClass = iValueClass;
        value = iValue;

        assert value!=null;
    }

    public static <C extends DataChanges<C>,U extends UsedChanges<C,U>> void dependsClasses(Collection<DataPropertyInterface> interfaces,C changes, U fill) {
        for(DataPropertyInterface valueInterface : interfaces) {
            fill.dependsAdd(changes, valueInterface.interfaceClass);
            fill.dependsRemove(changes, valueInterface.interfaceClass);
        }
    }

    <C extends DataChanges<C>,U extends UsedChanges<C,U>> U calculateUsedChanges(C changes, Collection<DataProperty> usedDefault, Depends<C, U> depends) {
        U result = depends.newChanges();
        dependsClasses(interfaces, changes, result);
        return result;
    }

    public static Where getIsClassWhere(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {
        Where classWhere = Where.TRUE;
        for(Map.Entry<DataPropertyInterface,? extends SourceExpr> join : joinImplement.entrySet()) // берем (нужного класса and не remove'уты) or add'уты
            classWhere = classWhere.and(DataSession.getIsClassWhere(session, join.getValue(),
                    join.getKey().interfaceClass, changedWhere));
        return classWhere;
    }

    public SourceExpr calculateSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {
        // здесь session может быть null
        return new ValueExpr(value,valueClass).and(getIsClassWhere(joinImplement, session, usedDefault, depends, changedWhere));
    }
}
