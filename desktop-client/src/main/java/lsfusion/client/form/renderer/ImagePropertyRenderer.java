package lsfusion.client.form.renderer;

import lsfusion.client.Main;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class ImagePropertyRenderer extends FilePropertyRenderer {

    public ImagePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value == null && property.isEditableNotNull()) {
            setText(REQUIRED_STRING);
            setForeground(REQUIRED_FOREGROUND);
            setIcon(null);
        } else {
            setIcon(value == null ? null : new ImageIcon((byte[]) value));
            setText(null);
        }
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

            Dimension scaled = scaleIcon(icon, width, height);
            if (scaled == null) {
                return;
            }
            int imageWidth = scaled.width;
            int imageHeight = scaled.height;

            int dx = (width - imageWidth) / 2;
            int dy = (height - imageHeight) / 2;
            
            g.drawImage(img, dx, dy, imageWidth, imageHeight, this);
        } else {
            super.paintComponent(g);
        }
    }
    
    public static Dimension scaleIcon(ImageIcon icon, int boundWidth, int boundHeight) {
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

    public static void expandImage(final byte[] value) {
        if (value == null) {
            return;
        }
        final JDialog dialog = new JDialog(Main.frame, true);

        ActionListener escListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                dialog.setVisible(false);
            }
        };
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        dialog.getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        final Rectangle bounds = Main.frame.getBounds();
        bounds.x += 30;
        bounds.y += 30;
        bounds.width -= 60;
        bounds.height -= 60;
        dialog.setBounds(bounds);
        dialog.setResizable(false);

        Image image = Toolkit.getDefaultToolkit().createImage(value);
        ImageIcon imageIcon = new ImageIcon(image);
        if (imageIcon.getIconWidth() > bounds.width || imageIcon.getIconHeight() > bounds.height) {
            Dimension scaled = scaleIcon(imageIcon, bounds.width, bounds.height);
            if (scaled != null) {
                imageIcon.setImage(image.getScaledInstance(scaled.width, scaled.height, Image.SCALE_SMOOTH));
            }
        }

        dialog.add(new JLabel(imageIcon));

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
