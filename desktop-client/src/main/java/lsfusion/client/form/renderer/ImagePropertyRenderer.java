package lsfusion.client.form.renderer;

import lsfusion.client.Main;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class ImagePropertyRenderer extends FilePropertyRenderer {
    private ImageIcon icon;

    public ImagePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);
        
        if (value != null) {
            icon = new ImageIcon((byte[]) value);
        } else {
            icon = null;
        }
    }

    @Override
    public void paintLabelComponent(Graphics g) {
        super.paintLabelComponent(g);

        if (icon != null) {
            paintComponent(getComponent(), g, icon);
        }
    }
    
    public static void paintComponent(JComponent component, Graphics g, ImageIcon icon) {
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

    public static void expandImage(final byte[] value) {
        if (value != null) {
            Image image = Toolkit.getDefaultToolkit().createImage(value);
            if (image != null) {
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
    }
}
