package lsfusion.server.base.caches;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.translator.MapValuesTranslate;

public abstract class AbstractTranslateValues<T extends TranslateValues<T>> extends TwinImmutableObject implements TranslateValues<T> {

    public T translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
    }
}
