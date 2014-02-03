package lsfusion.server.caches;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Аннотация, при которой теоретически strong не обязателен (!!! то есть нет доступа к gc root, глобальным ресурсам), но его отстутствие приведет к большому количеству дополнительных кэшей для результата (а потенциально приведет и к утечке памяти) 
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface IdentityInstanceLazy {
}
