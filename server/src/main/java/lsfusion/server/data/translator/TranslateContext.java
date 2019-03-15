package lsfusion.server.data.translator;

public interface TranslateContext<T extends TranslateContext<T>> {

    T translateOuter(MapTranslate translator);
}
