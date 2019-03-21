package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GJSONType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeJSONFileCaption();
    }
}
