package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.navigator.GNavigatorElement;

public class NavigatorImageButton extends ImageButton {

    public NavigatorImageButton(GNavigatorElement element, boolean vertical) {
        super(element.caption, element.image, vertical);
    }
}
