package lsfusion.server.caches;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// кэши, которые очень интенсивно используются при старте сервера, а в последствии редко только для используемых
// как правило только для высокоуровневых кэшей, так как идет привязка к бизнес-логике что весьма не быстрый процесс
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface IdentityStartLazy {
}
