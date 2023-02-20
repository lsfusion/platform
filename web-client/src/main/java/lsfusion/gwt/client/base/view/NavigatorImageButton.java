package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.navigator.GNavigatorElement;

public class NavigatorImageButton extends ImageButton {

    private final GNavigatorElement element;

    public NavigatorImageButton(GNavigatorElement element, boolean vertical) {
        super(element.caption, element.image, vertical, Document.get().createAnchorElement());

        this.element = element;
    }

    @Override
    protected BaseImage getImage() {
        return element.image;
    }
}
