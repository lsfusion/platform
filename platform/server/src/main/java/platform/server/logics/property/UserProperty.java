package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ConcreteClass;
import platform.server.classes.UnknownClass;
import platform.server.classes.ValueClass;
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

    @Override
    public ClassWhere<Object> getClassValueWhere() {
        return new ClassWhere<Object>(BaseUtils.<Object, ValueClass>add(IsClassProperty.getMapClasses(interfaces), "value", getValueClass()), true);
    }

    public DerivedChange<?,?> derivedChange = null;
    
    protected Set<Property> getDerivedDepends() {
        return derivedChange !=null ? derivedChange.getDepends() : new HashSet<Property>();
    }

    @IdentityLazy
    protected PropertyImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    @IdentityLazy
    protected PropertyImplement<?, String> getValueClassProperty() {
        return IsClassProperty.getProperty(getValueClass(), "value");
    }

    protected Set<Property> getClassDepends() {
        return BaseUtils.<Property>toSet(getInterfaceClassProperty().property, getValueClassProperty().property);
    }

    @Override
    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(getClassDepends());
    }


    private boolean fitClass(ConcreteClass concreteClass, ValueClass valueClass) {
        // unknown, custom, concatenateClassSet
        if(concreteClass instanceof ValueClass)
            return valueClass.isCompatibleParent((ValueClass) concreteClass);
        else {
            assert concreteClass instanceof UnknownClass; // с concatenate'ами надо будет разбираться
            return false;
        }
    }
    public boolean fitClasses(Map<ClassPropertyInterface, ConcreteClass> mapValues, ConcreteClass value) { // оптимизация
        if(value!=null && !fitClass(value, getValueClass()))
            return false;
        for(Map.Entry<ClassPropertyInterface, ConcreteClass> interfaceValue : mapValues.entrySet())
            if(!fitClass(interfaceValue.getValue(), interfaceValue.getKey().interfaceClass))
                return false;
        return true;
    }

    @Override
    protected MapDataChanges<ClassPropertyInterface> calculateDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        change = change.and(getInterfaceClassProperty().mapExpr(change.getMapExprs(), propChanges, null).getWhere().and(getValueClassProperty().mapExpr(Collections.singletonMap("value", change.expr), propChanges, null).getWhere().or(change.expr.getWhere().not())));
        if(change.where.isFalse()) // чтобы не плодить пустые change'и
            return new MapDataChanges<ClassPropertyInterface>();

        if(changedWhere !=null) changedWhere.add(change.where); // помечаем что можем обработать тока подходящие по интерфейсу классы
        return newDataChanges(change);
    }

    public MapDataChanges<ClassPropertyInterface> newDataChanges(PropertyChange<ClassPropertyInterface> change) {
        return new MapDataChanges<ClassPropertyInterface>(new DataChanges(this, change), Collections.singletonMap(this, getIdentityInterfaces()));
    }

    public abstract ValueClass getValueClass();

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
