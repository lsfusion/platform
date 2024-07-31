package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;

public class GWordLinkType extends GLinkType {
    @Override
    public String getExtension() {
        return "doc";
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeWordFileLinkCaption();
    }
}