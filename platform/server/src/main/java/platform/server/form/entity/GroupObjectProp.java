package platform.server.form.entity;

import platform.server.classes.*;

public enum GroupObjectProp {
    FILTER, ORDER, VIEW;

    public String getSID() {
        if(this==FILTER)
            return "FILTER";
        if(this==VIEW)
            return "VIEW";
        return "ORDER";
    }

    public DataClass getValueClass() {
        if(this==ORDER)
            return TextClass.instance;
        return LogicalClass.instance;
    }

}
