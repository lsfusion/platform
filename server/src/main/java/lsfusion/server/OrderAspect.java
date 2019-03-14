package lsfusion.server;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;

// Pack делаем до кэширования, чтобы не зависали ссылки на ключи в LRU кэшах
@Aspect
@DeclarePrecedence("lsfusion.server.base.remote.context.RemoteContextAspect, lsfusion.server.base.remote.RemoteExceptionsAspect, lsfusion.server.physics.admin.logging.RemoteLoggerAspect, PackComplexityAspect, CacheAspect, lsfusion.server.data.translator.AfterTranslateAspect, AutoHintsAspect, QueryCacheAspect, lsfusion.server.data.query.MapCacheAspect, WrapComplexityAspect, ExecutionStackAspect")
public class OrderAspect {
}
