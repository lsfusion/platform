package lsfusion.server.logics.property.actions.integration.hierarchy;

import lsfusion.base.Pair;

// there are two strategies working with hierarchy : JSON - edge-based, и XML - node-based, we'll use edge-based approach, because in xml attr and maps are also more edge-based then node-based  
public interface Node<T extends Node<T>> {
    // import
    T getNode(String key); // group
    
    String getValue(String key, boolean attr); // property
        
    Iterable<Pair<Object, T>> getMap(String key, boolean isIndex); // group object
    
    // export
    T createNode(); 
        
    void addNode(T node, String key, T childNode); // group 
    
    void addValue(T node, String key, boolean attr, String value); // property

    void addMap(T node, String key, boolean isIndex, Iterable<Pair<Object, T>> map); // group object
}
