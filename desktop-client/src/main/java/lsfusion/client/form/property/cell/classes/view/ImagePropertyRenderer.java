package lsfusion.client.form.property.cell.classes.view;

import com.google.common.base.Throwables;
import lsfusion.base.file.AppFileDataImage;
import lsfusion.base.file.RawFileData;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.view.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class ImagePropertyRenderer extends FilePropertyRenderer {
    private ImageIcon icon;

    public ImagePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);

        setIcon(value != null ? convertValue(((AppFileDataImage) value)) : null);
    }

    protected void setIcon(Image image) {
        this.icon = image != null ? new ImageIcon(image) : null;
    }

    @Override
    public void paintLabelComponent(Graphics g) {
        super.paintLabelComponent(g);

        if (icon != null) {
            paintComponent(getComponent(), g, icon, property);
        }
    }
    
    public static void paintComponent(JComponent component, Graphics g, ImageIcon icon, ClientPropertyDraw property) {
        assert icon != null;

        int width = component.getWidth();
        int height = component.getHeight();

        if (width == 0 || height == 0) {
            return;
        }

        Image img = icon.getImage();

        Dimension scaled = getIconScale(icon, width, height);
        if (scaled == null) {
            return;
        }
        int imageWidth = scaled.width;
        int imageHeight = scaled.height;

        int dx = (width - imageWidth) / 2;
        if (property != null && property.valueAlignmentHorz != null) {
            switch (property.valueAlignmentHorz) {
                case START:
                    dx = 0;
                    break;
                case END:
                    dx = width - imageWidth;
            }
        }
        
        int dy = (height - imageHeight) / 2;

        g.drawImage(img, dx, dy, imageWidth, imageHeight, component);
    } 
    
    public static Dimension getIconScale(ImageIcon icon, int boundWidth, int boundHeight) {
        int imageWidth = icon.getIconWidth();
        int imageHeight = icon.getIconHeight();
        if (imageWidth == 0 || imageHeight == 0) {
            return null;
        }

        double cf = imageWidth / (double)imageHeight;

        if (cf * boundHeight <= boundWidth) {
            //влезли по высоте
            return new Dimension((int) (cf * boundHeight), boundHeight);
        } else {
            return new Dimension(boundWidth, (int) (boundWidth / cf));
        }
    } 

    public static void expandImage(final AppFileDataImage value) {
        if (value != null) {
            expandImage(convertValue(value));
        }
    }

    protected static void expandImage(Image image) {
        if (image != null) {
            final JDialog dialog = new JDialog(MainFrame.instance, true);
            dialog.getRootPane().registerKeyboardAction(actionEvent -> dialog.setVisible(false),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

            final Rectangle bounds = MainFrame.instance.getBounds();
            bounds.x += 30;
            bounds.y += 30;
            bounds.width -= 60;
            bounds.height -= 60;
            dialog.setBounds(bounds);
            dialog.setResizable(false);

            ImageIcon imageIcon = new ImageIcon(image);
            if (imageIcon.getIconWidth() > bounds.width || imageIcon.getIconHeight() > bounds.height) {
                Dimension scaled = getIconScale(imageIcon, bounds.width, bounds.height);
                if (scaled != null) {
                    imageIcon.setImage(image.getScaledInstance(scaled.width, scaled.height, Image.SCALE_SMOOTH));
                }
            }

            dialog.add(new JLabel(imageIcon));

            dialog.pack();
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setVisible(true);
        }
    }

    public static Image convertValue(AppFileDataImage value) {
        try {
            return ImageIO.read(ImageIO.createImageInputStream(RawFileData.toRawFileData(value.data).getInputStream()));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return null;
    }
}
