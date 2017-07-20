package lsfusion.server.logics.mutables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// обозначает multithread использование, не включен IDGenerator так как в другом модуле
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface NFLazy {
}
