package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.ClientMessages;

public class GCustomDynamicFormatFileType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeCustomDynamicFormatFileCaption();
    }
}
