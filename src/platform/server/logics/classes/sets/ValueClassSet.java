package platform.server.logics.classes.sets;

import platform.Main;
import platform.base.SubNodeSet;
import platform.server.logics.properties.PropertyInterface;

import java.util.*;

// по сути Map<InterfaceClass,ClassSet>, а точнее даже UniMap<InterfaceClass,ClassSet>
public class ValueClassSet<P extends PropertyInterface> extends SubNodeSet<ChangeClass<P>, ValueClassSet<P>> implements PropertyClass<P> {

    public ValueClassSet() {
    }

    public ValueClassSet(Set<ChangeClass<P>> iNodes) {
        super(iNodes);
    }

    public ValueClassSet(ClassSet value, InterfaceClassSet<P> interfaceSet) {
        super(new ChangeClass<P>(interfaceSet,value));
    }

    public void or(ChangeClass<P> toAdd) {

        for(Iterator<ChangeClass<P>> i = iterator();i.hasNext();) {
            ChangeClass<P> changeClass = i.next();
            // также надо сделать что если совпадают классы то просто за or'ить
            // если мн-во классов включает другое мн-во, то уберем избыточные InterfaceClass
            if(changeClass.value.containsAll(toAdd.value))
                toAdd = new ChangeClass<P>(toAdd.interfaceClasses.excludeAll(changeClass.interfaceClasses),toAdd.value);
            else
            if(toAdd.value.containsAll(changeClass.value))
                changeClass.interfaceClasses.removeAll(toAdd.interfaceClasses);
            else {
                // также надо по хорошему если вижу одинаковые InterfaceClass сделать OR их ClassSet'ов и вырезать их в отдельный ChangeClass
            }
            // уберем старый класс если надо
            if(changeClass.interfaceClasses.isEmpty()) i.remove();
        }
        if(!toAdd.interfaceClasses.isEmpty())
            add(toAdd);
    }

    public Set<ChangeClass<P>> and(ChangeClass<P> andNode, ChangeClass<P> node) {
        ClassSet andValue = andNode.value.copy();
        andValue.or(node.value);
        return Collections.singleton(new ChangeClass<P>(andNode.interfaceClasses.and(node.interfaceClasses),andValue));
    }

    public ValueClassSet<P> create(Set<ChangeClass<P>> iNodes) {
        return new ValueClassSet<P>(iNodes);
    }

    Map<InterfaceClass<P>,ClassSet> cacheValueClass = new HashMap<InterfaceClass<P>, ClassSet>();
    public ClassSet getValueClass(InterfaceClass<P> interfaceImplement) {
        ClassSet result = null;
        if(Main.activateCaches) result = cacheValueClass.get(interfaceImplement);
        if(result==null) {
            result = new ClassSet();
            for(ChangeClass<P> changeClass : this) // если пересекается хоть с одним, то на выходе может иметь что угодно
                if(!changeClass.interfaceClasses.and(interfaceImplement).isEmpty())
                    result.or(changeClass.value);
            if(Main.activateCaches) cacheValueClass.put(interfaceImplement,result);
        }
        return result;
    }

    Map<ClassSet,InterfaceClassSet<P>> cacheClassSet = new HashMap<ClassSet,InterfaceClassSet<P>>();
    public InterfaceClassSet<P> getClassSet(ClassSet reqValue) {
        InterfaceClassSet<P> result = null;
        if(Main.activateCaches) result = cacheClassSet.get(reqValue);
        if(result==null) {
            result = new InterfaceClassSet<P>();
            for(ChangeClass<P> ChangeClass : this) // по сути надо если пересекается ReqValue
                if(reqValue ==ClassSet.universal || ChangeClass.value.intersect(reqValue))
                    result.or(ChangeClass.interfaceClasses);
            if(Main.activateCaches) cacheClassSet.put(reqValue,result);
        }
        return result;
    }

    public ValueClassSet<P> getValueClassSet() {
        return this;
    }

    public <V extends PropertyInterface> ValueClassSet<V> map(Map<P,V> mapInterfaces) {
        ValueClassSet<V> result = new ValueClassSet<V>();
        for(ChangeClass<P> change : this)
            result.add(change.map(mapInterfaces));
        return result;
    }

    public <V extends PropertyInterface> ValueClassSet<V> mapBack(Map<V,P> mapInterfaces) {
        ValueClassSet<V> result = new ValueClassSet<V>();
        for(ChangeClass<P> change : this)
            result.add(change.mapBack(mapInterfaces));
        return result;
    }

}
