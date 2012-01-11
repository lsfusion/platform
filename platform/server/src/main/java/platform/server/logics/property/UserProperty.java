package platform.server.logics.property;

import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
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
        return new CommonClasses<ClassPropertyInterface>(ClassProperty.getMapClasses(interfaces), getValueClass());
    }

    @Override
    protected PropertyChanges calculateUsedDataChanges(PropertyChanges propChanges) {
        return ClassProperty.getIsClassUsed(interfaces, propChanges).add(ClassProperty.getIsClassUsed(getValueClass(), propChanges));
    }

    @Override
    protected MapDataChanges<ClassPropertyInterface> calculateDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        change = change.and(ClassProperty.getIsClassWhere(change.mapKeys, propChanges, null).and(ClassProperty.getIsClassWhere(getValueClass(), change.expr, propChanges, null).or(change.expr.getWhere().not())));
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

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) {
        if(derived && derivedChange !=null) derivedChange.fillDepends(depends);
    }

    public DerivedChange<?,?> derivedChange = null;

}
