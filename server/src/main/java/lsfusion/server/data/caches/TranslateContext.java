package lsfusion.server.data.caches;

import lsfusion.server.data.translator.MapTranslate;

public interface TranslateContext<T extends TranslateContext<T>> {

    T translateOuter(MapTranslate translator);
}
