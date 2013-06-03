package lsfusion.server.form.entity;

import lsfusion.server.classes.*;

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
            return OrderClass.value;
        return LogicalClass.instance;
    }

}
