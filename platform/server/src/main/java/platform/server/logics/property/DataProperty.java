package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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

    public QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return QuickSet.EMPTY();
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

    @Override
    public PropertyChange<ClassPropertyInterface> getDerivedChange(PropertyChanges newChanges, PropertyChanges prevChanges) {
        PropertyChange<ClassPropertyInterface> result = null;

        Map<ClassPropertyInterface, KeyExpr> mapKeys = getMapKeys();
        Expr prevExpr = null;
        for(ClassPropertyInterface remove : interfaces) {
            IsClassProperty classProperty = remove.interfaceClass.getProperty();
            if(classProperty.hasChanges(newChanges) || classProperty.hasChanges(prevChanges)) {
                if(prevExpr==null) // оптимизация
                    prevExpr = getExpr(mapKeys);
                result = PropertyChange.addNull(result, new PropertyChange<ClassPropertyInterface>(mapKeys, classProperty.getRemoveWhere(mapKeys.get(remove), newChanges, prevChanges).and(prevExpr.getWhere())));
            }
        }
        IsClassProperty classProperty = value.getProperty();
        if(classProperty.hasChanges(newChanges) || classProperty.hasChanges(prevChanges)) {
            if(prevExpr==null) // оптимизация
                prevExpr = getExpr(mapKeys);
            result = PropertyChange.addNull(result, new PropertyChange<ClassPropertyInterface>(mapKeys, classProperty.getRemoveWhere(prevExpr, newChanges, prevChanges)));
        }
        
        return PropertyChange.addNull(result, super.getDerivedChange(newChanges, prevChanges));
    }

    @Override
    public QuickSet<Property> getUsedDerivedChange(StructChanges propChanges) {
        QuickSet<Property> result = super.getUsedDerivedChange(propChanges).merge(value.getProperty().getUsedChanges(propChanges));
        for(ClassPropertyInterface remove : interfaces)
            result = result.merge(remove.interfaceClass.getProperty().getUsedChanges(propChanges));
        if(derivedChange!=null)
            result = result.merge(derivedChange.getUsedDataChanges(propChanges, true));
        return result;
    }

    @Override
    public PropertyChange<ClassPropertyInterface> getDerivedChange(PropertyChanges propChanges) {
        return PropertyChange.addNull(getDerivedChange(propChanges, PropertyChanges.EMPTY), super.getDerivedChange(propChanges));
    }
    
    // не сильно структурно поэтому вынесено в метод
    public <V> Map<ClassPropertyInterface, V> getMapInterfaces(List<V> list) {
        int i=0;
        Map<ClassPropertyInterface, V> result = new HashMap<ClassPropertyInterface, V>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface, list.get(i++));
        return result;
    }
}
