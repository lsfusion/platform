package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.type.Type;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public abstract class UserProperty extends Property<ClassPropertyInterface> {

    public static List<ClassPropertyInterface> getInterfaces(ValueClass[] classes) {
        List<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new ClassPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    protected UserProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, getInterfaces(classes));
    }

    public CommonClasses<ClassPropertyInterface> getCommonClasses() {
        return new CommonClasses<ClassPropertyInterface>(IsClassProperty.getMapClasses(interfaces), getValueClass());
    }

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) {
        if(derived && derivedChange !=null) depends.addAll(derivedChange.getNewDepends());
    }

    public DerivedChange<?,?> derivedChange = null;

    @IdentityLazy
    private PropertyImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    @IdentityLazy
    private PropertyImplement<?, String> getValueClassProperty() {
        return IsClassProperty.getProperty(getValueClass(), "value");
    }

    @Override
    public Set<Property> getChangeDepends() {
        return BaseUtils.mergeSet(super.getChangeDepends(), BaseUtils.<Property>toSet(getInterfaceClassProperty().property, getValueClassProperty().property));
    }

    @Override
    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(getChangeDepends());
    }

    @Override
    protected MapDataChanges<ClassPropertyInterface> calculateDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        change = change.and(getInterfaceClassProperty().mapExpr(change.mapKeys, propChanges, null).getWhere().and(getValueClassProperty().mapExpr(Collections.singletonMap("value", change.expr), propChanges, null).getWhere().or(change.expr.getWhere().not())));
        if(change.where.isFalse()) // чтобы не плодить пустые change'и
            return new MapDataChanges<ClassPropertyInterface>();

        if(changedWhere !=null) changedWhere.add(change.where); // помечаем что можем обработать тока подходящие по интерфейсу классы
        // изменяет себя, если классы совпадают
        return new MapDataChanges<ClassPropertyInterface>(new DataChanges(this, change), Collections.singletonMap(this, getIdentityInterfaces()));
    }

    public Type getType() {
        return getValueClass().getType();
    }

    public abstract ValueClass getValueClass();

    @IdentityLazy
    public ClassWhere<Field> getClassWhere(PropertyField storedField) {
        Map<Field, AndClassSet> result = new HashMap<Field, AndClassSet>();
        for(Map.Entry<ClassPropertyInterface, KeyField> mapKey : mapTable.mapKeys.entrySet())
            result.put(mapKey.getValue(), mapKey.getKey().interfaceClass.getUpSet());
        result.put(storedField, getValueClass().getUpSet());
        return new ClassWhere<Field>(result);
    }

    protected boolean useSimpleIncrement() {
        return false;
    }

    public abstract void execute(ExecutionContext context) throws SQLException;

    public PropertyChange<ClassPropertyInterface> getDerivedChange(Modifier newModifier, Modifier prevModifier) {
        return getDerivedChange(newModifier.getPropertyChanges(), prevModifier.getPropertyChanges());
    }

    public PropertyChange<ClassPropertyInterface> getDerivedChange(PropertyChanges newChanges, PropertyChanges prevChanges) {
        if(derivedChange!=null && derivedChange.hasUsedDataChanges(newChanges, prevChanges)) {
            PropertyChange<ClassPropertyInterface> propertyChange = derivedChange.getDataChanges(newChanges, prevChanges).get(this);
            return propertyChange != null ? propertyChange : getNoChange(); // noChange для прикола с stored в aspectgetexpr (там hasChanges стоит, а с null'ом его не будет)
        }
        return null;
    }
}
