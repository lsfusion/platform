package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

public class DataPropertyInterface extends PropertyInterface<DataPropertyInterface> {
    public ValueClass interfaceClass;

    public DataPropertyInterface(int ID, ValueClass iClass) {
        super(ID);
        interfaceClass = iClass;

        this.keyExpr = new KeyExpr("DC"+ID);
    }

    // для того чтобы "попробовать" изменения
    public final KeyExpr keyExpr;

    public static Map<DataPropertyInterface, KeyExpr> getMapKeys(Collection<DataPropertyInterface> interfaces) {
        Map<DataPropertyInterface,KeyExpr> result = new HashMap<DataPropertyInterface, KeyExpr>();
        for(DataPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.keyExpr);
        return result;
    }
    
}
