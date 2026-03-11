package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GXMLType extends GAXMLType {
    public static GXMLType instance = new GXMLType();

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeXMLCaption();
    }
}
