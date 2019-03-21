package lsfusion.gwt.shared.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GHTMLType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeHTMLFileCaption();
    }
}

