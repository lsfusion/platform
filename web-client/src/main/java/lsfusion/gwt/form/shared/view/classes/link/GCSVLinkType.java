package lsfusion.gwt.form.shared.view.classes.link;

import lsfusion.gwt.form.client.MainFrameMessages;

public class GCSVLinkType extends GLinkType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeCSVFileLinkCaption();
    }
}