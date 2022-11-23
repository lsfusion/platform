package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.BaseStaticImage;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

public abstract class ImageButton extends FormButton implements ColorThemeChangeListener {

    private boolean focusable = true;

    protected Element imageElement;

    protected BaseStaticImage baseImage;
    protected BaseStaticImage overrideImage;

    public ImageButton(String caption, BaseStaticImage baseImage, boolean vertical, Element element) {
        super(element);

        addStyleName("btn-image");

        this.baseImage = baseImage; // need to do it before setText for the updateStyle call

        setText(caption);

        if(baseImage != null) {
            // we want useBootstrap to be initialized (to know what kind of image should be used)
            if(MainFrame.staticImagesURL != null)
                initImage(baseImage, vertical);
        }

//        setFocusable(false);
    }

    private void initImage(BaseStaticImage baseImage, boolean vertical) {
        imageElement = baseImage.createImage();

        imageElement.addClassName("btn-image-img");
        getElement().insertFirst(imageElement);

        if (vertical)
            imageElement.addClassName("wrap-img-vert-margins");
        else
            imageElement.addClassName("wrap-img-horz-margins");

        MainFrame.addColorThemeChangeListener(this);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        updateStyle(text);
    }

    private void updateStyle(String text) {
        if(baseImage != null) { // optimization
            if (text != null && !text.isEmpty()) {
                addStyleName("wrap-text-not-empty");
            } else {
                removeStyleName("wrap-text-not-empty");
            }
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
