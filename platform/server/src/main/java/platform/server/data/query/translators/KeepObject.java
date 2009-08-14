package platform.server.data.query.translators;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.util.Map;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface KeepObject {
}
