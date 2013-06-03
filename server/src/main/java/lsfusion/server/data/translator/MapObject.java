package lsfusion.server.data.translator;

import lsfusion.server.caches.AbstractTranslateContext;

public interface MapObject {
    public AbstractTranslateContext aspectGetCache(AbstractTranslateContext context);
    public void aspectSetCache(AbstractTranslateContext context, AbstractTranslateContext result);
}
