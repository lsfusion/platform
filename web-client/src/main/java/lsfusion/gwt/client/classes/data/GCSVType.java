package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GCSVType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeCSVFileCaption();
    }
}
