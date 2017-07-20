package lsfusion.gwt.base.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;

public final class DivWidget extends Widget {
    public DivWidget() {
        setElement(Document.get().createDivElement());
    }
}
