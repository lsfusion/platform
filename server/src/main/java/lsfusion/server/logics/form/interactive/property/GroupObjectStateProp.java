package lsfusion.server.logics.form.interactive.property;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;

public enum GroupObjectStateProp implements GroupObjectProp {
    ISSELECT, VIEWTYPE;

    @Override
    public String getSID() {
        if(this == ISSELECT)
            return "ISSELECT";
        return "VIEWTYPE";
    }

    public ValueClass getValueClass(ConcreteCustomClass listViewType) {
        if(this == VIEWTYPE)
            return listViewType;
        return LogicalClass.instance;
    }
}
