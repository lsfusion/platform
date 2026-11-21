package lsfusion.http;

import java.lang.reflect.Proxy;
import java.util.function.BiFunction;

// a fake of an interface - a servlet request, response, session -, each call answered by the method's name and arguments
public class Fakes {

    @SuppressWarnings("unchecked")
    public static <T> T fake(Class<T> type, BiFunction<String, Object[], Object> answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> answer.apply(method.getName(), args));
    }
}
