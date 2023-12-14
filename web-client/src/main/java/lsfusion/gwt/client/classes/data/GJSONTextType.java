package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GJSONTextType extends GAJSONType {
    public static GJSONTextType instance = new GJSONTextType();

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeJSONTextCaption();
    }
}
