package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;

public class GTableLinkType extends GLinkType {
    @Override
    public String getExtension() {
        return "table";
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeXMLFileLinkCaption();
    }
}