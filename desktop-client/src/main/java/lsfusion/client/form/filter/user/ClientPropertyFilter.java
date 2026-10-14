package lsfusion.client.form.filter.user;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.view.Column;
import lsfusion.interop.form.property.Compare;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyFilter {

    public ClientFilter filter;
    public ClientGroupObject groupObject;
    // the property in one of its columns; no column key - the column of the current objects of the column groups
    public Column column;
    public ClientDataFilterValue value;

    public boolean negation;
    public Compare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

    public ClientPropertyFilter(ClientFilter filter, ClientGroupObject groupObject, ClientGroupObjectValue columnKey, Object value) {
        this(filter, groupObject, columnKey, value, null, null, null);
    }
    public ClientPropertyFilter(ClientFilter filter, ClientGroupObject groupObject, ClientGroupObjectValue columnKey, Object value, Boolean negation, Compare compare, Boolean junction) {
        this.filter = filter;
        this.groupObject = groupObject;
        this.column = new Column(filter.property, columnKey);
        this.value = new ClientDataFilterValue(value);
        if (negation != null) {
            this.negation = negation;
        }
        this.compare = compare != null ? compare : filter.property.getDefaultCompare();
        if (junction != null) {
            this.junction = junction;
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(column.property.getID());
        outStream.writeBoolean(column.columnKey != null);
        if(column.columnKey != null)
            column.columnKey.serialize(outStream);
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
    
    public void override(ClientPropertyFilter filter) {
        compare = filter.compare;
        junction = filter.junction;
        negation = filter.negation;
        value.setValue(filter.value.value);
    }
}
