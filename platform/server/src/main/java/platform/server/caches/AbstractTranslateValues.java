package platform.server.caches;

import platform.base.TwinImmutableObject;
import platform.server.data.translator.MapValuesTranslate;

public abstract class AbstractTranslateValues<T extends TranslateValues<T>> extends TwinImmutableObject implements TranslateValues<T> {

    public T translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
    }
}
