package lsfusion.server.logics.form.interactive;

import lsfusion.server.classes.*;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.logics.classes.LogicalClass;
import lsfusion.server.logics.classes.OrderClass;

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
