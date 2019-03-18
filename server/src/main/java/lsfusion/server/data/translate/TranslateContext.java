package lsfusion.server.data.translate;

public interface TranslateContext<T extends TranslateContext<T>> {

    T translateOuter(MapTranslate translator);
}
