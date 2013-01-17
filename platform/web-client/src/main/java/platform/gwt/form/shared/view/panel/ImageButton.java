package platform.gwt.form.shared.view.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.ImageDescription;

public class ImageButton extends Button {
    private final Image image;
    private final Label label;
    private Widget strut;
    private final CellPanel panel;

    private String imagePath;
    private String text;

    public ImageButton() {
        this(null, null, false);
    }

    public ImageButton(String caption) {
        this(caption, null, false);
    }

    public ImageButton(String caption, String imagePath) {
        this(caption, imagePath, false);
    }

    public ImageButton(String caption, ImageDescription imageDescription) {
        this(caption, null, false);
        setImage(imageDescription);
    }

    public ImageButton(String caption, boolean directionBottom) {
        this(caption, null, directionBottom);
    }

    public ImageButton(String caption, String imagePath, boolean directionBottom) {
        panel = directionBottom ? new VerticalPanel() {} : new HorizontalPanel();
        panel.add(image = new Image());
        image.setVisible(false);
        if (!directionBottom) {
            strut = GwtClientUtils.createHorizontalStrut(2);
            panel.add(strut);
        }
        panel.add(label = new Label());

        if (directionBottom) {
            panel.setCellHorizontalAlignment(image, HasHorizontalAlignment.ALIGN_CENTER);
            panel.setCellHorizontalAlignment(label, HasHorizontalAlignment.ALIGN_CENTER);
        }

        label.addStyleName("customFontPresenter");

        setText(caption);
        setRelativeImagePath(imagePath);

        getElement().appendChild(panel.getElement());
    }

    public void setImage(ImageDescription imageDescription) {
        if (imageDescription != null) {
            setAbsoluteImagePath(imageDescription.url);
            if (imageDescription.width != -1) {
                image.setWidth(imageDescription.width + "px");
            }
            if (imageDescription.height != -1) {
                image.setHeight(imageDescription.height + "px");
            }
        }
    }

    public void setRelativeImagePath(String imagePath) {
        setAbsoluteImagePath(imagePath == null ? null : GWT.getModuleBaseURL() + "images/" + imagePath);
    }

    public void setAbsoluteImagePath(String imagePath) {
        if (!GwtSharedUtils.nullEquals(this.imagePath, imagePath)) {
            image.setUrl(imagePath == null ? "" : GWT.getHostPageBaseURL() + imagePath);

            if ((this.imagePath == null && imagePath != null) || (this.imagePath != null && imagePath == null)) {
                image.setVisible(imagePath != null);
                updateStrut();
            }

            this.imagePath = imagePath;
        }
    }

    public void setText(String text) {
        if (!GwtSharedUtils.nullEquals(this.text, text)) {

            label.setText(text);
            if ((this.text == null && text != null) || (this.text != null && text == null)) {
                label.setVisible(text != null && !text.isEmpty());
                updateStrut();
            }
            this.text = text;
        }
    }

    private void updateStrut() {
        if (strut != null) {
            strut.setVisible(image.isVisible() && label.isVisible());
        }
    }
}
