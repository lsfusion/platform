package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.MainFrameMessages;

public class GPDFType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typePDFFileCaption();
    }
}
