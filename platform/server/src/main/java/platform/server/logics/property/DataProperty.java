package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.add;

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

    public PropertyChange<ClassPropertyInterface> getDerivedChange(PropertyChanges changes) {
        PropertyChange<ClassPropertyInterface> result = null;

        Map<ClassPropertyInterface, KeyExpr> mapKeys = getMapKeys();
        Expr prevExpr = null;
        for(ClassPropertyInterface remove : interfaces) {
            IsClassProperty classProperty = remove.interfaceClass.getProperty();
            if(classProperty.hasChanges(changes)) {
                if(prevExpr==null) // оптимизация
                    prevExpr = getExpr(mapKeys);
                result = PropertyChange.addNull(result, new PropertyChange<ClassPropertyInterface>(mapKeys, classProperty.getRemoveWhere(mapKeys.get(remove), changes).and(prevExpr.getWhere())));
            }
        }
        IsClassProperty classProperty = value.getProperty();
        if(classProperty.hasChanges(changes)) {
            if(prevExpr==null) // оптимизация
                prevExpr = getExpr(mapKeys);
            result = PropertyChange.addNull(result, new PropertyChange<ClassPropertyInterface>(mapKeys, classProperty.getRemoveWhere(prevExpr, changes)));
        }

        if(derivedChange!=null && derivedChange.hasEventChanges(changes)) {
            PropertyChange<ClassPropertyInterface> propertyChange = derivedChange.getDataChanges(changes).get(this);
            result = PropertyChange.addNull(result, propertyChange != null ? propertyChange : getNoChange()); // noChange для прикола с stored в aspectgetexpr (там hasChanges стоит, а с null'ом его не будет)
        }

        return result;
    }

    @Override
    public QuickSet<Property> getUsedDerivedChange(StructChanges propChanges) {
        QuickSet<Property> result = super.getUsedDerivedChange(propChanges).merge(value.getProperty().getUsedChanges(propChanges));
        for(ClassPropertyInterface remove : interfaces)
            result = result.merge(remove.interfaceClass.getProperty().getUsedChanges(propChanges));
        if(derivedChange!=null && derivedChange.hasEventChanges(propChanges))
            result = result.merge(derivedChange.getUsedDataChanges(propChanges));
        return result;
    }

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) { // для Action'а связь считается слабой
        if(derived) depends.addAll(getDerivedDepends());
    }

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        return add(add(super.calculateLinks(),
                new Pair<Property<?>, LinkType>(getInterfaceClassProperty().property, LinkType.ACTIONDERIVED)),
                new Pair<Property<?>, LinkType>(getValueClassProperty().property, LinkType.ACTIONDERIVED)); // чтобы удаления классов зацеплять
    }
}
