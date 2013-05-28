package platform.server.form.instance;

import platform.server.data.type.Type;
import platform.server.form.instance.filter.CompareValue;

public interface OrderInstance extends CompareValue {

    // даже если не в интерфейсе все равно ставить (то есть по сути фильтр делать false)
    public final static boolean ignoreInInterface = true;

    GroupObjectInstance getApplyObject();

    Type getType();
    
}
