package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.GwtClientUtils;

public class LabelWidget extends Widget {

    public LabelWidget() {
        setElement(Document.get().createLabelElement());
        GwtClientUtils.addClassName(this, "gwt-Label");
    }
}
