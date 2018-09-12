package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;

public class GPDFType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typePDFFileCaption();
    }
}
