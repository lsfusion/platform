package platform.server.logics.properties;

import platform.server.data.classes.ValueClass;
import platform.server.data.query.exprs.KeyExpr;

public class DataPropertyInterface extends PropertyInterface<DataPropertyInterface> {
    public ValueClass interfaceClass;

    public DataPropertyInterface(int ID, ValueClass iClass) {
        super(ID);
        interfaceClass = iClass;

        this.keyExpr = new KeyExpr("DC"+ID);
    }

    // для того чтобы "попробовать" изменения
    public final KeyExpr keyExpr;
}
