package lsfusion.server.form.instance;

import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.filter.CompareInstance;

public interface OrderInstance extends CompareInstance {

    // даже если не в интерфейсе все равно ставить (то есть по сути фильтр делать false)
    boolean ignoreInInterface = true;

    GroupObjectInstance getApplyObject();

    Type getType();
    
}
