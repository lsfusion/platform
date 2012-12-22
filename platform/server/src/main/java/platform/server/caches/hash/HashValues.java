package platform.server.caches.hash;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

public abstract class HashValues extends HashObject {

    public abstract int hash(Value expr);

    public abstract HashValues filterValues(ImSet<Value> values);

    public abstract HashValues reverseTranslate(MapValuesTranslate translate, ImSet<Value> values);
}
