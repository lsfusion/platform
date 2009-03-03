package platform.server.logics.classes.sets;

import platform.server.logics.ObjectValue;
import platform.server.logics.properties.PropertyInterface;

import java.util.HashMap;
import java.util.Map;

public class InterfaceClass<P extends PropertyInterface> extends HashMap<P,ClassSet> {

    public InterfaceClass(Map<P, ObjectValue> keys) {
        for(Map.Entry<P,ObjectValue> key : keys.entrySet())
            put(key.getKey(),new ClassSet(key.getValue().objectClass));
    }

    public InterfaceClass() {
    }

    public InterfaceClass(P propertyInterface, ClassSet interfaceValue) {
        put(propertyInterface,interfaceValue);
    }

    // когда каждый ClassSet включает в себя все подмн-ва
    public boolean isParent(InterfaceClass<P> node) {
        // здесь строго говоря доджен keySet'ы должны совпадать
        // DEBUG проверка
        if(!keySet().equals(node.keySet()))
            throw new RuntimeException("different interfaces");
        for(Map.Entry<P,ClassSet> propertyInterface : entrySet())
            if(!propertyInterface.getValue().containsAll(node.get(propertyInterface.getKey()))) return false;
        return true;
    }

    public InterfaceClass<P> and(InterfaceClass<P> node) {
        InterfaceClass<P> and = new InterfaceClass<P>();
        and.putAll(node);
        for(Map.Entry<P,ClassSet> propertyInterface : entrySet()) {
            ClassSet andClass = and.get(propertyInterface.getKey());
            if(andClass==null)
                andClass = propertyInterface.getValue();
            else
                andClass = andClass.and(propertyInterface.getValue());
            and.put(propertyInterface.getKey(),andClass);
        }
        return and;
    }

    <V extends PropertyInterface> InterfaceClass<V> map(Map<P,V> mapInterfaces) {
        InterfaceClass<V> result = new InterfaceClass<V>();
        for(Map.Entry<P,V> propertyInterface : mapInterfaces.entrySet())
            result.put(propertyInterface.getValue(),get(propertyInterface.getKey()));
        return result;
    }

    public <V extends PropertyInterface> InterfaceClass<V> mapBack(Map<V,P> mapInterfaces) {
        InterfaceClass<V> result = new InterfaceClass<V>();
        for(Map.Entry<V,P> propertyInterface : mapInterfaces.entrySet())
            result.put(propertyInterface.getKey(),get(propertyInterface.getValue()));
        return result;
    }

    public boolean hasEmpty() {
        for(ClassSet set : values())
            if(set.isEmpty()) return true;
        return false;
    }
}
