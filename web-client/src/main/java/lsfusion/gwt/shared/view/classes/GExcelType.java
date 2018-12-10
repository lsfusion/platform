package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;

public class GExcelType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeExcelFileCaption();
    }
}
