package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.MainFrameMessages;

public class GTableType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeTableFileCaption();
    }
}
