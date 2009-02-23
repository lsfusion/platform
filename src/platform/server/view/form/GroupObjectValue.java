package platform.server.view.form;

import java.util.Map;

public class GroupObjectValue extends GroupObjectMap<Integer> {

    public GroupObjectValue() {}
    GroupObjectValue(Map<ObjectImplement,Integer> iValue) {
        putAll(iValue);
    }
}
