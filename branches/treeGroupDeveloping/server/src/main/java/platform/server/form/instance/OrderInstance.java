package platform.server.form.instance;

import platform.server.data.type.Type;
import platform.server.form.instance.filter.CompareValue;

public interface OrderInstance extends CompareValue {

    GroupObjectInstance getApplyObject();

    Type getType();
    
}
