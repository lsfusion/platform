package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.BaseStaticImage;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.navigator.view.GToolbarNavigatorView;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

public abstract class ImageButton extends FormButton implements ColorThemeChangeListener {

    private boolean focusable = true;

    protected Element imageElement;

    protected BaseStaticImage baseImage;

    public ImageButton(String caption, BaseStaticImage baseImage) {
        setStyleName("btn-image");

        setText(caption);
        updateStyle(caption);

        this.baseImage = baseImage;

        if(baseImage != null) {
            imageElement = baseImage.createImage();

            imageElement.addClassName("btn-image-img");
            getElement().insertFirst(imageElement);

            MainFrame.addColorThemeChangeListener(this);
        }

//        setFocusable(false);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        updateStyle(text);
    }

    private void updateStyle(String text) {
        if (text != null && !text.equals("")) {
            removeStyleName("btn-image-no-caption");
        } else {
            addStyleName("btn-image-no-caption");
        }
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
        baseImage.setImageSrc(imageElement);
    }
}
