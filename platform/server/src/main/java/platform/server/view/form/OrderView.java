package platform.server.view.form;

import platform.server.view.form.filter.CompareValue;
import platform.server.data.types.Type;
import platform.server.logics.properties.Property;

import java.util.Set;

public interface OrderView extends CompareValue {

    GroupObjectImplement getApplyObject();

    Type getType();
    
}
