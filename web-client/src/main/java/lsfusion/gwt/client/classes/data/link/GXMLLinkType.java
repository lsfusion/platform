package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;

public class GXMLLinkType extends GLinkType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTableFileLinkCaption();
    }
}