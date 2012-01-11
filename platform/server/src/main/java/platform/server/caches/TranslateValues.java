package platform.server.caches;

import platform.server.data.translator.MapValuesTranslate;

public interface TranslateValues<T extends TranslateValues<T>> {

    T translateValues(MapValuesTranslate translate);

}
