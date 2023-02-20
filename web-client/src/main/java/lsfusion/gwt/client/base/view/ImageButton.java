package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

public abstract class ImageButton extends FormButton implements ColorThemeChangeListener {

    private boolean focusable = true;

    private boolean vertical;

    public ImageButton(String caption, BaseImage image, boolean vertical, Element element) {
        super(element);

        addStyleName("btn-image");
        addStyleName("btn-outline-secondary");

        this.vertical = vertical;

        BaseImage.initImageText(this, caption, image, vertical);

//        imgElement.addClassName("btn-image-img"); ???

        MainFrame.addColorThemeChangeListener(this);
    }

    protected abstract BaseImage getImage();

    protected void updateImage() {
        BaseImage.updateImage(getImage(), this, vertical);
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

    @Override
    public void colorThemeChanged() {
        updateImage();
    }
}
