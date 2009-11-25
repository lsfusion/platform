package platform.server.view.form;

import platform.server.data.type.Type;
import platform.server.view.form.filter.CompareValue;

public interface OrderView extends CompareValue {

    GroupObjectImplement getApplyObject();

    Type getType();
    
}
