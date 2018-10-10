package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;

public class GJSONType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeJSONFileCaption();
    }
}
