package lsfusion.interop;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * copy/paste сериализации из ImageIcon
 */
public class SerializableImageIconHolder implements Serializable {
    public static final long serialVersionUID = 42L;

    private ImageIcon image;

    public SerializableImageIconHolder(ImageIcon image) {
        this.image = image;
    }

    public ImageIcon getImage() {
        return image;
    }

    public void setImage(ImageIcon image) {
        this.image = image;
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        if (s.readBoolean()) {
            int w = s.readInt();
            int h = s.readInt();
            int[] pixels = (int[]) (s.readObject());

            if (pixels != null) {
                Toolkit tk = Toolkit.getDefaultToolkit();
                ColorModel cm = ColorModel.getRGBdefault();
                image = new ImageIcon(tk.createImage(new MemoryImageSource(w, h, cm, pixels, 0, w)));
            }
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeBoolean(image != null);
        if (image != null) {
            int w = image.getIconWidth();
            int h = image.getIconHeight();
            int[] pixels = image.getImage() != null ? new int[w * h] : null;

            if (image.getImage() != null) {
                try {
                    PixelGrabber pg = new PixelGrabber(image.getImage(), 0, 0, w, h, pixels, 0, w);
                    pg.grabPixels();
                    if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
                        throw new IOException("failed to load image contents");
                    }
                } catch (InterruptedException e) {
                    throw new IOException("image load interrupted");
                }
            }

            s.writeInt(w);
            s.writeInt(h);
            s.writeObject(pixels);
        }
    }
}
