package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;

public class GTXTLinkType extends GLinkType {

    @Override
    public String getExtension() {
        return "txt";
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTextFileLinkCaption();
    }
}