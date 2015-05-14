package lsfusion.client.remote.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// временно, все методы с этой аннотацией должны быть в FormClientAction.methodNames
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ImmutableMethod {
}
