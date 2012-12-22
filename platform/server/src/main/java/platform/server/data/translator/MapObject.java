package platform.server.data.translator;

import platform.server.caches.AbstractTranslateContext;

public interface MapObject {
    public AbstractTranslateContext aspectGetCache(AbstractTranslateContext context);
    public void aspectSetCache(AbstractTranslateContext context, AbstractTranslateContext result);
}
