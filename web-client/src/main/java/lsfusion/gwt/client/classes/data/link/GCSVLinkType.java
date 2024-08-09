package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;

public class GCSVLinkType extends GLinkType {
    @Override
    public String getExtension() {
        return "csv";
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeCSVFileLinkCaption();
    }
}