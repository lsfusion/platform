package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;

public class GCSVType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeCSVFileCaption();
    }
}
