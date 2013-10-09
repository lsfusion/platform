package lsfusion.client.form.renderer;

import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;

public class ImagePropertyRenderer extends FilePropertyRenderer {

    public ImagePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        setIcon(value == null ? null : new ImageIcon((byte[]) value));
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) {
            return;
        }

        ImageIcon icon = (ImageIcon) getIcon();
        if (icon != null) {
            Image img = icon.getImage();

            int imageWidth = icon.getIconWidth();
            int imageHeight = icon.getIconHeight();
            if (imageWidth == 0 || imageHeight == 0) {
                return;
            }

            double cf = imageWidth / (double)imageHeight;

            if (cf * height <= width) {
                //влезли по высоте
                imageHeight = height;
                imageWidth = (int) (cf * height);
            } else {
                imageWidth = width;
                imageHeight = (int) (width / cf);
            }

            int dx = (width - imageWidth) / 2;
            int dy = (height - imageHeight) / 2;

            g.drawImage(img, dx, dy, imageWidth, imageHeight, this);
        } else {
            super.paintComponent(g);
        }
    }
}
