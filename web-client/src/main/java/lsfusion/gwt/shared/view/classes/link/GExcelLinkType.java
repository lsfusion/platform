package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.form.MainFrameMessages;

public class GExcelLinkType extends GLinkType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeExcelFileLinkCaption();
    }
}