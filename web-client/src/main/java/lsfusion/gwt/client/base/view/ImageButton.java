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
    protected BaseStaticImage overrideImage;

    public ImageButton(String caption, BaseStaticImage baseImage) {
        setStyleName("btn-image");

        setText(caption);
        updateStyle(caption);

        this.baseImage = baseImage;

        if(baseImage != null) {
            // we want useBootstrap to be initialized (to know what kind of image should be used)
            if(MainFrame.staticImagesURL != null)
                initImage(baseImage);
            else
                MainFrame.staticImagesURLListeners.add(() -> {
                    initImage(baseImage);

                    if(overrideImage != null) // overrideImage could be set
                        updateImageSrc();
                });
        }

//        setFocusable(false);
    }

    private void initImage(BaseStaticImage baseImage) {
        imageElement = baseImage.createImage();

        imageElement.addClassName("btn-image");
        getElement().insertFirst(imageElement);

        MainFrame.addColorThemeChangeListener(this);
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

    public void changeImage(StaticImage overrideImage) {
        this.overrideImage = overrideImage;
        updateImageSrc();
    }

    @Override
    public void colorThemeChanged() {
        updateImageSrc();
    }

    private void updateImageSrc() {
        if(imageElement != null) // might not be initialized, see the constructor
            baseImage.setImageSrc(imageElement, overrideImage);
    }
}
