package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.Pair;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.ParseException;

// there are two strategies working with hierarchy : JSON - edge-based, и XML - node-based, we'll use edge-based approach, because in xml attr and maps are also more edge-based then node-based  
public interface Node<T extends Node<T>> {
    // import
    T getNode(String key); // group
    
    Object getValue(String key, boolean attr, Type type) throws ParseException; // property
        
    Iterable<Pair<Object, T>> getMap(String key, boolean isIndex); // group object
    
    // export
    T createNode(); 
    
    boolean isUpDown(); // if we need to first add element to structure (for example for namespace resolving in XML) and then fill it or vice versa
        
    void addNode(T node, String key, T childNode); // group 
    void removeNode(T node, T childNode); // group, it can be called only for upDown formats 
    
    void addValue(T node, String key, boolean attr, boolean extNull, Object value, Type type); // property

    boolean addMap(T node, String key, boolean isIndex, Iterable<Pair<Object, T>> map); // group object
}
