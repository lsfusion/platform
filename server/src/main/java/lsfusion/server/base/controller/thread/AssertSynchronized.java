package lsfusion.server.base.controller.thread;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// проверяет что метод вызывается thread safe

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface AssertSynchronized {
}
