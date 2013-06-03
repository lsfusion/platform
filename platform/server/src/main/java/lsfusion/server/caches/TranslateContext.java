package lsfusion.server.caches;

import lsfusion.server.data.translator.MapTranslate;

public interface TranslateContext<T extends TranslateContext<T>> {

    T translateOuter(MapTranslate translator);
}
