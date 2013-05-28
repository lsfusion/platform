package platform.interop.remote;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

public class MethodInvocation implements Serializable {
    public final String name;
    public final Class[] params;
    public final Object[] args;
    public final Class retClass;

    public MethodInvocation(String name, Class[] params, Object[] args, Class retClass) {
        this.params = params;
        this.name = name;
        this.retClass = retClass;
        this.args = args;
    }

    @Override
    public String toString() {
        return retClass.getSimpleName() + " " + name + "(" + Arrays.toString(params) + ')';
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MethodInvocation) {
            MethodInvocation invocation = (MethodInvocation) other;
            return name.equals(invocation.name) && Arrays.equals(args, invocation.args) && Arrays.equals(params, invocation.params);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * name.hashCode() + Arrays.hashCode(params)) + Arrays.hashCode(args);
    }

    public static MethodInvocation create(Class clazz, String name, Object... args) {
        for (Method method : clazz.getMethods()) {
            //todo : надо бы еще проверять и на сами классы, если у методов будет одинаковое имя и количество параметров
            if (method.getName().equals(name) && method.getParameterTypes().length == args.length) {
                return new MethodInvocation(name, method.getParameterTypes(), args, method.getReturnType());
            }
        }

        return null;
    }
}
