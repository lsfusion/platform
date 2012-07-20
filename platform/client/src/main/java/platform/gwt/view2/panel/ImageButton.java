package platform.gwt.view2.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class ImageButton extends Button {
    private final Image image;
    private final Label label;
    private final HorizontalPanel panel;

    public ImageButton() {
        this(null, null);
    }

    public ImageButton(String caption) {
        this(caption, caption);
    }

    public ImageButton(String caption, String imagePath) {
        panel = new HorizontalPanel();
        panel.add(image = new Image());
        panel.add(label = new Label());

        setText(caption);
        setImagePath(imagePath);
    }

    public void setImagePath(String imagePath) {
        setAbsoluteImagePath(imagePath == null ? null : GWT.getModuleBaseURL() + "images/" + imagePath);
    }

    public void setAbsoluteImagePath(String imagePath) {
        image.setUrl(imagePath == null ? "" : imagePath);
        refreshHTML();
    }

    public void setText(String text) {
        label.setText(text);
        refreshHTML();
    }

    private void refreshHTML() {
        setHTML(panel.getElement().getString());
    }
}
