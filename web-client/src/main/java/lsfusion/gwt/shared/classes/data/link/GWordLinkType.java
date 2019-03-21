package lsfusion.gwt.shared.classes.data.link;

import lsfusion.gwt.client.ClientMessages;

public class GWordLinkType extends GLinkType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeWordFileLinkCaption();
    }
}