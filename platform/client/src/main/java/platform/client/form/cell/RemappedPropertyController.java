package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientObject;
import platform.client.logics.ClientPropertyDraw;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class RemappedPropertyController extends PropertyController {
    protected ClientGroupObjectValue columnKey;
    protected RemappedClientPropertyDraw remappedKey;

    public RemappedPropertyController(ClientPropertyDraw ikey, ClientFormController form, ClientGroupObjectValue columnKey) {
        super(new RemappedClientPropertyDraw(ikey, columnKey), form);

        this.columnKey = columnKey;
        this.remappedKey = (RemappedClientPropertyDraw) key;
    }

    public boolean cellValueChanged(Object ivalue) {

        try {
            form.changePropertyDrawWithColumnKeys(getKey(), ivalue, false, columnKey);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при изменении значения свойства", e);
        }

        return true;
    }

    
    public void setDisplayValues(Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnDisplayValues) {
        remappedKey.setDisplayValues(columnDisplayValues);
        keyUpdated();
    }

    public static class RemappedClientPropertyDraw extends ClientPropertyDraw {
        public ClientPropertyDraw original;
        public ClientGroupObjectValue columnKey;
        private String columnKeysCaption;

        public RemappedClientPropertyDraw(ClientPropertyDraw original, ClientGroupObjectValue columnKey) {
            super(original);
            this.original = original;
            this.columnKey = columnKey;
            this.columnKeysCaption = "";
        }

        public String getFullCaption() {
            return super.getFullCaption() + columnKeysCaption;
        }

        public void setDisplayValues(Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnDisplayValues) {
            if (columnKey != null && !columnKey.isEmpty()) {
                String paramCaption = "";
                Iterator<Map.Entry<ClientObject, Object>> columnKeysIt = columnKey.entrySet().iterator();
                for (int j = 0; j < columnGroupObjects.length; ++j) {
                    ClientPropertyDraw columnProperty = columnDisplayProperties[j];
                    ClientGroupObject columnGroupObject = columnGroupObjects[j];

                    ClientGroupObjectValue partColumnKey = new ClientGroupObjectValue();
                    for (int k = 0; k < columnGroupObject.size(); ++k) {
                        Map.Entry<ClientObject, Object> entry = columnKeysIt.next();
                        partColumnKey.put(entry.getKey(), entry.getValue());
                    }
                    if (paramCaption.length() != 0) {
                        paramCaption += ", ";
                    }
                    paramCaption += columnDisplayValues.get(columnProperty).get(partColumnKey);
                }

                columnKeysCaption = "[" + paramCaption + "]";
            }
        }
    }
}