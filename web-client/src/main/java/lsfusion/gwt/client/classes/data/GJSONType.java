package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GJSONType extends GAJSONType {
    public static GJSONType instance = new GJSONType();

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeJSONCaption();
    }
}
