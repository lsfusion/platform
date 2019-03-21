package lsfusion.gwt.shared.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.shared.GwtSharedUtils;

public class GCustomStaticFormatFileType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeCustomStaticFormatFileCaption() + ": " + GwtSharedUtils.toString(",", validContentTypes != null ? validContentTypes.toArray() : "");
    }
}
