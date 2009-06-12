package platform.server.logics.properties;

import platform.server.data.classes.ConcreteValueClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.types.Type;
import platform.server.session.*;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;
import platform.base.BaseUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ClassProperty extends AggregateProperty<ClassPropertyInterface> {

    ConcreteValueClass valueClass;
    Object value;

    static Collection<ClassPropertyInterface> getInterfaces(ValueClass[] classes) {
        Collection<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new ClassPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    public ClassProperty(String iSID, ValueClass[] classes, ConcreteValueClass iValueClass, Object iValue) {
        super(iSID, getInterfaces(classes));
        
        valueClass = iValueClass;
        value = iValue;
    }

    protected boolean fillDependChanges(List<Property> changedProperties, DataChanges changes, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {
        // если Value null то ничего не интересует
        if(value ==null) return false;
        if(changes==null) return true;
        
        for(ClassPropertyInterface valueInterface : interfaces)
            if(valueInterface.interfaceClass instanceof CustomClass && (changes.getAddClasses().contains((CustomClass)valueInterface.interfaceClass) ||
                    changes.getRemoveClasses().contains((CustomClass)valueInterface.interfaceClass)))
                return true;

        return false;
    }

    @Override
    public void fillTableChanges(TableChanges fill, TableChanges changes) {
        super.fillTableChanges(fill, changes);
        for(ClassPropertyInterface propertyInterface : interfaces)
            if(propertyInterface.interfaceClass instanceof CustomClass) {
                BaseUtils.putNotNull((CustomClass)propertyInterface.interfaceClass,changes.remove.get((CustomClass)propertyInterface.interfaceClass),fill.remove);
                BaseUtils.putNotNull((CustomClass)propertyInterface.interfaceClass,changes.add.get((CustomClass)propertyInterface.interfaceClass),fill.add);
            }
    }

    public SourceExpr calculateSourceExpr(Map<ClassPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {
        // здесь session может быть null

        if(value==null) return new CaseExpr();

        Where classWhere = Where.TRUE;
        for(ClassPropertyInterface valueInterface : interfaces) // берем (нужного класса and не remove'уты) or add'уты
            classWhere = classWhere.and(DataSession.getIsClassWhere(session, joinImplement.get(valueInterface),
                    valueInterface.interfaceClass, changedWhere));
        return new CaseExpr(classWhere,new ValueExpr(value,valueClass));
    }
}
