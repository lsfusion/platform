package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.form.MainFrameMessages;

public class GJSONLinkType extends GLinkType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeJSONFileLinkCaption();
    }
}