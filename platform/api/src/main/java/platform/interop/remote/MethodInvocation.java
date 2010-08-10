package platform.interop.remote;

import java.io.Serializable;
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
        return retClass + " " + name + "(" + Arrays.asList(args) + ')';
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
}
