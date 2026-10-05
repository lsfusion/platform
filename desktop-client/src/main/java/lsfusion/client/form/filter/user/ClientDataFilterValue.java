package lsfusion.client.form.filter.user;

import lsfusion.base.BaseUtils;
import lsfusion.client.ClientResourceBundle;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientDataFilterValue {

    public Object value;

    public ClientDataFilterValue() {
    }
    
    public ClientDataFilterValue(Object value) {
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        BaseUtils.serializeObject(outStream, value);
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.value");
    }
}
