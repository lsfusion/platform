package lsfusion.client.form.filter.user;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyFilter {

    public ClientGroupObject groupObject;
    public ClientPropertyDraw property;
    public ClientDataFilterValue value;

    public ClientGroupObjectValue columnKey; // nullable означает что надо текущий брать

    public boolean negation;
    public Compare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

    public ClientPropertyFilter(ClientGroupObject groupObject, ClientPropertyDraw property, ClientGroupObjectValue columnKey, Object value, Compare compare) {
        this(groupObject, property, new ClientDataFilterValue(value), columnKey, false, compare, true);
    }
    public ClientPropertyFilter(ClientGroupObject groupObject, ClientPropertyDraw property, ClientDataFilterValue value, ClientGroupObjectValue columnKey, boolean negation, Compare compare, boolean junction) {
        this.groupObject = groupObject;
        this.property = property;
        this.value = value;
        this.columnKey = columnKey;
        this.negation = negation;
        this.compare = compare;
        this.junction = junction;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(property.getID());
        outStream.writeBoolean(columnKey != null);
        if(columnKey != null)
            columnKey.serialize(outStream);
        outStream.writeBoolean(negation);
        compare.serialize(outStream);
        value.serialize(outStream);
        outStream.writeBoolean(junction);
    }

    public boolean nullValue() {
        return value.value == null;
    }
}
