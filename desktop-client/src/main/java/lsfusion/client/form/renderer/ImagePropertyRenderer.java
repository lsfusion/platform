package lsfusion.client.form.renderer;

import lsfusion.client.logics.ClientPropertyDraw;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImagePropertyRenderer extends FilePropertyRenderer {

    private byte[] value;

    public ImagePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        this.value = (byte[]) value;
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (value != null) {
            Image image = null;
            try {
                image = ImageIO.read(new ByteArrayInputStream(value));
                if (getWidth() != 0 && getHeight() != 0) {
                    double coef = Math.max((double)image.getWidth(null) / getWidth(), (double)image.getHeight(null) / getHeight());
                    image = coef > 1 ? image.getScaledInstance((int) (image.getWidth(null) / coef), (int) (image.getHeight(null) / coef), Image.SCALE_FAST) : image;
                }
            } catch (IOException ignored) {
            }

            if (image != null) {
                g.clearRect(0, 0, getWidth(), getHeight());
                int deltaWidth = (getWidth() - image.getWidth(null)) / 2;
                int deltaHeight = (getHeight() - image.getHeight(null)) / 2;
                g.drawImage(image, deltaWidth, deltaHeight, deltaWidth + image.getWidth(null), deltaHeight + image.getHeight(null),
                        0, 0, image.getWidth(null), image.getHeight(null), null);
            }
        }
    }
}
