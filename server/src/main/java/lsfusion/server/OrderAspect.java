package lsfusion.server;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;


@Aspect
@DeclarePrecedence("lsfusion.server.remote.RemoteContextAspect, lsfusion.server.remote.RemoteLoggerAspect, lsfusion.server.caches.CacheAspect, lsfusion.server.data.translator.AfterTranslateAspect, lsfusion.server.caches.AutoHintsAspect, lsfusion.server.caches.QueryCacheAspect, lsfusion.server.data.query.MapCacheAspect, lsfusion.server.caches.WrapComplexityAspect, lsfusion.server.MessageAspect, lsfusion.server.caches.PackComplexityAspect")
public class OrderAspect {
}
