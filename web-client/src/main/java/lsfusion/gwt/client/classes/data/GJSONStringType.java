package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GJSONStringType extends GAJSONType {
    public static GJSONStringType instance = new GJSONStringType();

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeJSONStringCaption();
    }
}
