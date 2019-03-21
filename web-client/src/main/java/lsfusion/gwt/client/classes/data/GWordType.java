package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GWordType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeWordFileCaption();
    }
}
