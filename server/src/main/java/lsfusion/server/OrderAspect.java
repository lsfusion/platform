package lsfusion.server;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;

// Pack делаем до кэширования, чтобы не зависали ссылки на ключи в LRU кэшах
@Aspect
@DeclarePrecedence("lsfusion.server.base.controller.remote.context.RemoteContextAspect, " +
                   "lsfusion.server.base.controller.remote.RemoteExceptionsAspect, " +
                   "lsfusion.server.physics.admin.log.RemoteLoggerAspect, " +
                   "lsfusion.server.data.pack.PackComplexityAspect, " +
                   "lsfusion.server.base.caches.CacheAspect, " +
                   "lsfusion.server.data.translate.AfterTranslateAspect, " +
                   "lsfusion.server.data.caches.QueryCacheAspect, " +
                   "lsfusion.server.logics.property.caches.MapCacheAspect, " +
                   "lsfusion.server.physics.exec.hint.AutoHintsAspect, " + // we need this before MapCacheAspect to make preread and hint increment to be thrown before the result will be cached (and that way no hint check will be performed) + that way if hint is thrown it will be thrown faster - just right after cache check
                   "lsfusion.server.physics.exec.hint.WrapComplexityAspect, " +
                   "lsfusion.server.base.controller.stack.ExecutionStackAspect, " +
                   "lsfusion.server.physics.admin.log.sql.SQLSessionLoggerAspect") // we need this after ExecutionStackAspect to have relevant sql time
public class OrderAspect {
}
