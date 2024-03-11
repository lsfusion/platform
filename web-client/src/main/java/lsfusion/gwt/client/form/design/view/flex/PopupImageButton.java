package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ImageButton;

public class PopupImageButton extends ImageButton {
    String caption;
    BaseImage image;

    public PopupImageButton(String caption, BaseImage image) {
        super(caption, image, false, Document.get().createAnchorElement());
        this.caption = caption;
        this.image = image;

        addStyleName("nav-link navbar-text");
    }

    JavaScriptObject tippy;

    public void setClickHandler(Widget widget) {
        addClickHandler(clickEvent -> {
            if(tippy == null) {
                tippy = GwtClientUtils.showTippyPopup(null, getElement(), widget, () -> tippy = null);
            } else {
                GwtClientUtils.hideTippyPopup(tippy);
                tippy = null;
            }
        });
    }

    @Override
    protected BaseImage getImage() {
        return image;
    }

    @Override
    protected String getCaption() {
        return caption;
    }

    @Override
    protected boolean forceDiv() {
        return false;
    }
}