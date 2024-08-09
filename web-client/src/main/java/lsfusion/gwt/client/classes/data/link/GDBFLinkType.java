package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;

public class GDBFLinkType extends GLinkType {
    @Override
    public String getExtension() {
        return "dbf";
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDBFFileLinkCaption();
    }
}