package platform.server.logics.classes.sets;

import platform.base.GraphNodeSet;
import platform.server.logics.classes.DataClass;
import platform.server.logics.properties.PropertyInterface;

import java.util.Set;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public class InterfaceClassSet<P extends PropertyInterface> extends GraphNodeSet<InterfaceClass<P>,InterfaceClassSet<P>> {

    public InterfaceClassSet() {
    }

    protected InterfaceClassSet(Set<InterfaceClass<P>> iNodes) {
        super(iNodes);
    }

    public InterfaceClassSet(InterfaceClass<P> node) {
        super(Collections.singleton(node));
    }

    public boolean has(InterfaceClass<P> orNode, InterfaceClass<P> node) {
        return orNode.hasEmpty() || node.isParent(orNode);
    }

    public boolean isEmpty() {
        return super.isEmpty() || (size()==1 && iterator().next().hasEmpty());
    }

    public Set<InterfaceClass<P>> and(InterfaceClass<P> andNode, InterfaceClass<P> node) {
        return Collections.singleton(andNode.and(node));
    }

    public InterfaceClassSet<P> create(Set<InterfaceClass<P>> iNodes) {
        return new InterfaceClassSet<P>(iNodes);
    }

    public Map<P, DataClass> getCommonParent() {
        Map<P, DataClass> result = new HashMap<P, DataClass>();
        for(P propertyInterface : iterator().next().keySet()) {
            ClassSet commonClassSet = new ClassSet();
            for(InterfaceClass<P> node : this)
                commonClassSet.or(node.get(propertyInterface));
            result.put(propertyInterface,commonClassSet.getCommonClass());
        }
        return result;
    }

    public <V extends PropertyInterface> InterfaceClassSet<V> map(Map<P,V> mapInterfaces) {
        InterfaceClassSet<V> result = new InterfaceClassSet<V>();
        for(InterfaceClass<P> node : this)
            result.add(node.map(mapInterfaces));
        return result;
    }

    public <V extends PropertyInterface> InterfaceClassSet<V> mapBack(Map<V,P> mapInterfaces) {
        InterfaceClassSet<V> result = new InterfaceClassSet<V>();
        for(InterfaceClass<P> node : this)
            result.add(node.mapBack(mapInterfaces));
        return result;
    }
}
