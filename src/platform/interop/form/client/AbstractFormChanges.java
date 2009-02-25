package platform.interop.form.client;

import java.util.*;

public class AbstractFormChanges<T,V,Z> {

    public Map<T,Boolean> classViews = new HashMap<T, Boolean>();
    public Map<T,V> objects = new HashMap<T, V>();
    public Map<T, List<V>> gridObjects = new HashMap<T, List<V>>();
    public Map<Z,Map<V,Object>> gridProperties = new HashMap<Z, Map<V, Object>>();
    public Map<Z,Object> panelProperties = new HashMap<Z, Object>();
    public Set<Z> dropProperties = new HashSet<Z>();
}
