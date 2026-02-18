package lsfusion.server.logics.form.interactive.property;

import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.OrderClass;

public enum GroupObjectRowProp implements GroupObjectProp {
    FILTER, ORDER, VIEW, SELECT;

    @Override
    public String getSID() {
        if(this==FILTER)
            return "FILTER";
        if(this==VIEW)
            return "VIEW";
        if(this==SELECT)
            return "SELECT";
        return "ORDER";
    }

    public DataClass getValueClass() {
        if(this==ORDER)
            return OrderClass.value;
        return LogicalClass.instance;
    }

}
