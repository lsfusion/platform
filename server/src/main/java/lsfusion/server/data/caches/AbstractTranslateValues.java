package lsfusion.server.data.caches;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.translator.TranslateValues;

public abstract class AbstractTranslateValues<T extends TranslateValues<T>> extends TwinImmutableObject implements TranslateValues<T> {

    public T translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
    }
}
