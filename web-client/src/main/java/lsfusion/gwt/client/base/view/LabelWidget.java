package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

public class LabelWidget extends Widget {

    public LabelWidget() {
        setElement(Document.get().createLabelElement());
        setStyleName("gwt-Label");
    }
}
