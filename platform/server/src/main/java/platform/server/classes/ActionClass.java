package platform.server.classes;

import platform.interop.Data;

public class ActionClass extends LogicalClass {

    public static final ActionClass instance = new ActionClass(); 

    @Override
    public String toString() {
        return "Action";
    }

    @Override
    public byte getTypeID() {
        return Data.ACTION;
    }
}
