package platform.server.caches;

import platform.server.data.translator.MapTranslate;

public interface TranslateContext<T extends TranslateContext<T>> {

    T translateOuter(MapTranslate translator);
}
