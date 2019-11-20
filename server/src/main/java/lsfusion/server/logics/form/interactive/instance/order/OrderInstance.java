package lsfusion.server.logics.form.interactive.instance.order;

import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.interactive.instance.filter.CompareInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;

public interface OrderInstance extends CompareInstance {

    GroupObjectInstance getApplyObject();

    Type getType();
    
}
