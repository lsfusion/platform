package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.ClientMessages;

public class GExcelLinkType extends GLinkType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeExcelFileLinkCaption();
    }
}