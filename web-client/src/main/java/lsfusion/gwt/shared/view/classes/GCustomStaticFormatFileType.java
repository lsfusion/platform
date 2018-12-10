package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.GwtSharedUtils;

public class GCustomStaticFormatFileType extends GFileType {
    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeCustomStaticFormatFileCaption() + ": " + GwtSharedUtils.toString(",", extensions.toArray());
    }
}
