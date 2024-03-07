package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.ImageHtmlOrTextType;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;

public abstract class ImageButton extends FormButton {

    private boolean focusable = true;

    private boolean vertical;

    public ImageButton(String caption, BaseImage image, boolean vertical, Element element) {
        super(element);

        if(element == null)
            GToolbarView.styleToolbarItem(getElement());

        this.vertical = vertical;

        BaseImage.initImageText(this, caption, image, this.vertical ? ImageHtmlOrTextType.BUTTON_VERT : ImageHtmlOrTextType.BUTTON_HORZ);
    }

    protected abstract BaseImage getImage();
    protected abstract String getCaption();
    protected abstract boolean forceDiv();

    public void updateImage() {
        BaseImage.updateImage(getImage(), this);
    }

    public void updateText() {
        BaseImage.updateText(this, getCaption(), forceDiv());
    }

    // call before attach
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        if (!focusable) {
            setTabIndex(-1);
        }
    }
}
