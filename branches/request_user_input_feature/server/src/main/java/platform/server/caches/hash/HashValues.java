package platform.server.caches.hash;

import platform.base.ImmutableObject;
import platform.base.QuickSet;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

import java.util.Set;

public abstract class HashValues extends HashObject {

    public abstract int hash(Value expr);

    public abstract HashValues filterValues(QuickSet<Value> values);

    public abstract HashValues reverseTranslate(MapValuesTranslate translate, QuickSet<Value> values);
}
