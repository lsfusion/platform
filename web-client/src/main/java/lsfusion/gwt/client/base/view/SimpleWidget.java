package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;

public class SimpleWidget extends Widget {
    public SimpleWidget(String tagName) {
        setElement(Document.get().createElement(tagName));
    }
}
