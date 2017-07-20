package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.base.shared.GwtSharedUtils;

public class GCustomStaticFormatFileType extends GFileType {
    @Override
    public String toString() {
        return "Файл с расширением: " + GwtSharedUtils.toString(",", extensions.toArray());
    }
}
