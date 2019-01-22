package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.ClientMessages;

public class GPDFType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typePDFFileCaption();
    }
}
