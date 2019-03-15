package lsfusion.server.logics.form.interactive;

import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.OrderClass;

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
