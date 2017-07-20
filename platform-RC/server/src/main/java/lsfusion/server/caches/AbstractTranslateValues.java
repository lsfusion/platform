package lsfusion.server.caches;

import lsfusion.base.TwinImmutableObject;
import lsfusion.server.data.translator.MapValuesTranslate;

public abstract class AbstractTranslateValues<T extends TranslateValues<T>> extends TwinImmutableObject implements TranslateValues<T> {

    public T translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
    }
}
