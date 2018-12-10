package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.MainFrameMessages;

public class GHTMLLinkType extends GLinkType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeHTMLFileLinkCaption();
    }
}