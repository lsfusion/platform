package platform.server.data.translator;

import platform.base.QuickSet;
import platform.server.caches.AbstractTranslateContext;
import platform.server.caches.OuterContext;
import platform.server.data.Value;
import platform.server.data.query.innerjoins.GroupJoinsWheres;

import java.util.Set;

public interface MapObject {
    public AbstractTranslateContext aspectGetCache(AbstractTranslateContext context);
    public void aspectSetCache(AbstractTranslateContext context, AbstractTranslateContext result);
}
