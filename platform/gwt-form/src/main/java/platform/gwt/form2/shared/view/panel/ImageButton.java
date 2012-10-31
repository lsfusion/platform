package platform.gwt.form2.shared.view.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import platform.gwt.base.shared.GwtSharedUtils;

public class ImageButton extends Button {
    private final Image image;
    private final Label label;
    private final CellPanel panel;

    private String imagePath;
    private String text;

    public ImageButton() {
        this(null, null);
    }

    public ImageButton(String caption) {
        this(caption, caption);
    }

    public ImageButton(String caption, String imagePath) {
        this(caption, imagePath, false);
    }

    public ImageButton(String caption, String imagePath, boolean directionBottom) {
        panel = directionBottom ? new VerticalPanel() : new HorizontalPanel();
        panel.add(image = new Image());
        panel.add(label = new Label());

        if (directionBottom) {
            panel.setCellHorizontalAlignment(image, HasHorizontalAlignment.ALIGN_CENTER);
            panel.setCellHorizontalAlignment(label, HasHorizontalAlignment.ALIGN_CENTER);
        }

        label.addStyleName("customFontPresenter");

        setText(caption);
        setImagePath(imagePath);
    }

    public void setImagePath(String imagePath) {
        setAbsoluteImagePath(imagePath == null ? null : GWT.getModuleBaseURL() + "images/" + imagePath);
    }

    public void setAbsoluteImagePath(String imagePath) {
        if (!GwtSharedUtils.nullEquals(this.imagePath, imagePath)) {
            this.imagePath = imagePath;
            image.setUrl(imagePath == null ? "" : imagePath);
            refreshHTML();
        }
    }

    public void setText(String text) {
        if (!GwtSharedUtils.nullEquals(this.text, text)) {
            this.text = text;
            label.setText(text);
            refreshHTML();
        }
    }

    private void refreshHTML() {
        setHTML(panel.getElement().getString());
    }
}
