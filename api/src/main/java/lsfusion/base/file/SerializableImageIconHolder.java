package lsfusion.base.file;

import lsfusion.base.ResourceUtils;
import lsfusion.interop.base.view.ColorTheme;

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
import java.util.HashMap;
import java.util.Map;

import static lsfusion.interop.base.view.ColorTheme.DEFAULT;

public class SerializableImageIconHolder implements Serializable {
    public static final long serialVersionUID = 42L;

    private String imagePath; // path to default color theme image
    private Map<ColorTheme, ImageIcon> images = new HashMap<>();

    public SerializableImageIconHolder(ImageIcon image, String imagePath) {
        setImage(image, imagePath);
    }
    
    public Map<ColorTheme, ImageIcon> getImages() {
        return images;
    }

    // color themes are currently not supported in desktop client. return default
    public ImageIcon getImage() {
        return images.get(DEFAULT);
    }
    
    public ImageIcon getImage(ColorTheme colorTheme) {
        return images.get(colorTheme);
    }

    public void putImage(ColorTheme colorTheme, ImageIcon image) {
        images.put(colorTheme, image);
    }

    public void setImage(ImageIcon image, String imagePath) {
        this.imagePath = imagePath;
        
        for (ColorTheme colorTheme : ColorTheme.values()) {
            if (colorTheme.isDefault()) {
                images.put(DEFAULT, image);
            } else {
                ImageIcon themeImage = ResourceUtils.readImage(getImagePath(colorTheme));
                if (themeImage != null) {
                    images.put(colorTheme, themeImage);
                }
            }
        }
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public String getImagePath(ColorTheme colorTheme) {
        return colorTheme.getImagePath(imagePath);
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        images = new HashMap<>();
        imagePath = (String) s.readObject();
        int size = s.readInt();
        for (int i = 0; i < size; i++) {
            if (s.readBoolean()) {
                ColorTheme colorTheme = (ColorTheme) s.readObject();
                
                int w = s.readInt();
                int h = s.readInt();
                int[] pixels = (int[]) (s.readObject());

                if (pixels != null) {
                    Toolkit tk = Toolkit.getDefaultToolkit();
                    ColorModel cm = ColorModel.getRGBdefault();
                    images.put(colorTheme, new ImageIcon(tk.createImage(new MemoryImageSource(w, h, cm, pixels, 0, w))));
                }
            }  
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(imagePath);
        s.writeInt(images.size());
        for (Map.Entry<ColorTheme, ImageIcon> imagesEntry : images.entrySet()) {
            ImageIcon image = imagesEntry.getValue();
            s.writeBoolean(image != null);
            if (image != null) {
                s.writeObject(imagesEntry.getKey());
                
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
}
