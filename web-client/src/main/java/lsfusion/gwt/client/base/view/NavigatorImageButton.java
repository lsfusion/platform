package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.navigator.GNavigatorElement;

public class NavigatorImageButton extends ImageButton {

    private final GNavigatorElement element;

    public NavigatorImageButton(GNavigatorElement element, boolean vertical) {
        this(element, vertical, false);
    }
    public NavigatorImageButton(GNavigatorElement element, boolean vertical, boolean span) {
        super(element.caption, element.image, vertical, span ? Document.get().createSpanElement() : Document.get().createAnchorElement());

        this.element = element;
    }

    @Override
    protected BaseImage getImage() {
        return element.image;
    }

    @Override
    protected String getCaption() {
        return element.caption;
    }
}
