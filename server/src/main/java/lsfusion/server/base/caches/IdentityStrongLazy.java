package lsfusion.server.base.caches;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// techically it's not caches, if they are dropped (missing, not synchronized) it can lead to memory leaks and even to incorrect behaviour
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface IdentityStrongLazy {
}
