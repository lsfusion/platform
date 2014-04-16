package lsfusion.server.logics.mutables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// обозначает multithread использование
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface NFStaticLazy {
}
