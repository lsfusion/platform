package lsfusion.client.form.filter.user;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyFilter {

    public ClientFilter filter;
    public ClientGroupObject groupObject;
    public ClientPropertyDraw property;
    public ClientDataFilterValue value;

    public ClientGroupObjectValue columnKey; // nullable означает что надо текущий брать

    public boolean negation;
    public Compare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

    public ClientPropertyFilter(ClientFilter filter, ClientGroupObject groupObject, ClientGroupObjectValue columnKey, Object value) {
        this(filter, groupObject, columnKey, value, null, null, null);
    }
    public ClientPropertyFilter(ClientFilter filter, ClientGroupObject groupObject, ClientGroupObjectValue columnKey, Object value, Boolean negation, Compare compare, Boolean junction) {
        this.filter = filter;
        this.groupObject = groupObject;
        this.property = filter.property;
        this.value = new ClientDataFilterValue(value);
        this.columnKey = columnKey;
        if (negation != null) {
            this.negation = negation;
        }
        this.compare = compare != null ? compare : filter.property.getDefaultCompare();
        if (junction != null) {
            this.junction = junction;
        }
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

    public boolean isFixed() {
        return filter.fixed;
    }
}
