package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.GwtClientUtils.getModuleImagePath;
import static lsfusion.gwt.client.base.GwtClientUtils.setSignImage;
import static lsfusion.gwt.client.base.GwtSharedUtils.isRedundantString;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class ImageButton extends Button implements ColorThemeChangeListener {
    protected final Image image;
    private final Label label;
    private final Widget strut;
    private final CellPanel panel;

    protected String imagePath; // default
    private String text = "";

    private boolean focusable = true;
    private boolean vertical;

    public ImageButton() {
        this(null, null, false);
    }

    public ImageButton(String caption) {
        this(caption, null, false);
    }

    public ImageButton(String caption, String imagePath) {
        this(caption, imagePath, false);
    }

    public ImageButton(String caption, boolean directionBottom) {
        this(caption, null, directionBottom);
    }

    public ImageButton(String caption, boolean vertical, boolean alignCenter) {
        this(caption, null, vertical, alignCenter);
    }

    public ImageButton(String caption, String imagePath, boolean vertical) {
        this(caption, imagePath, vertical, true);
    }

    public ImageButton(String caption, String imagePath, boolean vertical, boolean alignCenter) {
        this.vertical = vertical;

        panel = vertical ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();

        image = new Image();
        image.setVisible(false);
        image.addStyleName("displayBlock");

        strut = vertical ? null : GwtClientUtils.createHorizontalStrut(2);

        label = new Label();
        label.setVisible(false);

        if (!vertical) {
            panel.add(strut);
        }
        panel.add(label);

        if (vertical) {
            panel.setCellHorizontalAlignment(label, HasHorizontalAlignment.ALIGN_CENTER);
        } else {
            panel.setCellVerticalAlignment(label, HasAlignment.ALIGN_MIDDLE);
        }
        if (alignCenter) {
            panel.getElement().getStyle().setProperty("margin", "auto");
        }

        setText(caption);
        setModuleImagePath(imagePath);

        updateStrut();
        updateStyle();
        getElement().appendChild(panel.getElement());

        MainFrame.addColorThemeChangeListener(this);
    }

    public void setModuleImagePath(String imagePath) {
        this.imagePath = imagePath;
        ensureAndSet(imagePath);
    }

    private void ensureAndSet(String imagePath) {
        if (imagePath != null && !colorTheme.isDefault()) {
            setSignImage(imagePath, this::setAbsoluteImagePath);
        } else {
            setAbsoluteImagePath(getModuleImagePath(imagePath));
        }
    }

    protected void setAbsoluteImagePath(String imagePath) {
        String oldUrl = image.getUrl();
        if (!oldUrl.equals(imagePath == null ? "" : imagePath)) {
            image.setUrl(imagePath == null ? "" : imagePath);

            if ((oldUrl.isEmpty() && imagePath != null) || (!oldUrl.isEmpty() && imagePath == null)) {
                image.setVisible(imagePath != null);
                updateStrut();

                if (imagePath == null) {
                    image.removeFromParent();
                } else {
                    ((InsertPanel) panel).insert(image, 0);
                    if (vertical) {
                        panel.setCellHorizontalAlignment(image, HasHorizontalAlignment.ALIGN_CENTER);
                    } else {
                        panel.setCellVerticalAlignment(image, HasAlignment.ALIGN_MIDDLE);
                    }
                }
            }
        }
    }

    public void setText(String text) {
        if (!GwtSharedUtils.nullEquals(this.text, text)) {
            label.setText(text);
            if ((isRedundantString(this.text) && !isRedundantString(text)) || ((!isRedundantString(this.text) && isRedundantString(text)))) {
                label.setVisible(text != null && !text.isEmpty());
                updateStrut();
                updateStyle();
            }
            this.text = text;
        }
    }

    private void updateStrut() {
        if (strut != null) {
            strut.setVisible(image.isVisible() && label.isVisible());
        }
    }

    private void updateStyle() {
        if (label.isVisible()) {
            removeStyleName("imageButtonWithoutCaption");
        } else {
            addStyleName("imageButtonWithoutCaption");
        }
    }

    public Label getLabel() {
        return label;
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
