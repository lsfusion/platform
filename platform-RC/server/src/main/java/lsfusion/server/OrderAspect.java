package lsfusion.server;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;

// Pack делаем до кэширования, чтобы не зависали ссылки на ключи в LRU кэшах
@Aspect
@DeclarePrecedence("lsfusion.server.remote.RemoteContextAspect, lsfusion.server.remote.RemoteLoggerAspect, lsfusion.server.caches.PackComplexityAspect, lsfusion.server.caches.CacheAspect, lsfusion.server.data.translator.AfterTranslateAspect, lsfusion.server.caches.AutoHintsAspect, lsfusion.server.caches.QueryCacheAspect, lsfusion.server.data.query.MapCacheAspect, lsfusion.server.caches.WrapComplexityAspect, lsfusion.server.stack.ExecutionStackAspect")
public class OrderAspect {
}
