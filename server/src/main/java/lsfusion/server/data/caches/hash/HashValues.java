package lsfusion.server.data.caches.hash;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.value.Value;

public abstract class HashValues extends HashObject {

    public abstract int hash(Value expr);

    public abstract HashValues filterValues(ImSet<Value> values);

    public abstract HashValues reverseTranslate(MapValuesTranslate translate, ImSet<Value> values);
}
