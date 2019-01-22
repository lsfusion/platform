package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.ClientMessages;

public class GXMLType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeXMLFileCaption();
    }
}
