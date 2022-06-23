package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.GwtSharedUtils.isRedundantString;

public class ImageButton extends Button implements ColorThemeChangeListener {
    protected final Image image;

    protected String imagePath; // default

    private boolean focusable = true;

    public ImageButton(String caption, String imagePath) {
        setStyleName("btn-image");

        setText(caption);
        updateStyle(caption);

        image = new Image();
        image.setVisible(false);
        image.setStyleName("btn-image-img");

        getElement().insertFirst(image.getElement());

        setModuleImagePath(imagePath);

        MainFrame.addColorThemeChangeListener(this);

//        setFocusable(false);
    }

    public void setModuleImagePath(String imagePath) {
        this.imagePath = imagePath;
        ensureAndSet(imagePath);
    }

    private void ensureAndSet(String imagePath) {
        GwtClientUtils.setThemeImage(imagePath, this::setAbsoluteImagePath);
    }

    protected void setAbsoluteImagePath(String imagePath) {
        if (imagePath != null && !imagePath.equals("")) {
            image.setVisible(true);
            image.setUrl(imagePath);
        } else {
            image.setVisible(false);
            image.setUrl("");
        }
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
        ensureAndSet(imagePath);
    }
}
