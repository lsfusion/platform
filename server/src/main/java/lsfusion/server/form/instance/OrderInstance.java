package lsfusion.server.form.instance;

import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.filter.CompareValue;

public interface OrderInstance extends CompareValue {

    // даже если не в интерфейсе все равно ставить (то есть по сути фильтр делать false)
    boolean ignoreInInterface = true;

    GroupObjectInstance getApplyObject();

    Type getType();
    
}
