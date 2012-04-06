package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.type.Type;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.table.MapKeysTable;
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

    public DerivedChange<?,?> derivedChange = null;
    
    protected Set<Property> getDerivedDepends() {
        return derivedChange !=null ? derivedChange.getDepends() : new HashSet<Property>();
    }

    @IdentityLazy
    private PropertyImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    @IdentityLazy
    private PropertyImplement<?, String> getValueClassProperty() {
        return IsClassProperty.getProperty(getValueClass(), "value");
    }

    @Override
    public Set<Property> getDataChangeProps() {
        return getClassDepends();
    }

    private Set<Property> getClassDepends() {
        return BaseUtils.<Property>toSet(getInterfaceClassProperty().property, getValueClassProperty().property);
    }

    @Override
    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(getClassDepends());
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

    public abstract ValueClass getValueClass();

    public ClassWhere<Field> getClassWhere(MapKeysTable<ClassPropertyInterface> mapTable, PropertyField storedField) {
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

    // не сильно структурно поэтому вынесено в метод
    public <V> Map<ClassPropertyInterface, V> getMapInterfaces(List<V> list) {
        int i=0;
        Map<ClassPropertyInterface, V> result = new HashMap<ClassPropertyInterface, V>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface, list.get(i++));
        return result;
    }
}
