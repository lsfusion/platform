package lsfusion.server.caches.hash;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapValuesTranslate;

public abstract class HashValues extends HashObject {

    public abstract int hash(Value expr);

    public abstract HashValues filterValues(ImSet<Value> values);

    public abstract HashValues reverseTranslate(MapValuesTranslate translate, ImSet<Value> values);
}
