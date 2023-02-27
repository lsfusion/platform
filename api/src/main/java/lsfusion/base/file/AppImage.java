package lsfusion.base.file;

import lsfusion.base.BaseUtils;
import lsfusion.interop.base.view.ColorTheme;

import java.io.Serializable;
import java.util.Map;

public class AppImage implements Serializable {
    public static final long serialVersionUID = 42L;

    public AppImage() {
    }

    public static final String INPUT_NEW = "new";
    public static final String INPUT_DIALOG = "dialog";
    public static final String INPUT_RESET = "reset";

    private String imagePath;
    private Map<ColorTheme, RawFileData> images;

    public String fontClasses;

    public AppImage(String fontClasses, String imagePath, Map<ColorTheme, RawFileData> images) {
        this.fontClasses = fontClasses;

        this.imagePath = imagePath;
        this.images = images;
    }

    public transient Object desktopClientImages;

    public RawFileData getImage(ColorTheme colorTheme) {
        if(images == null)
            return null;

        return images.get(colorTheme);
    }

    public String getImagePath() {
        return imagePath;
    }
    
    public static boolean isGif(String imagePath) {
        return "gif".equalsIgnoreCase(BaseUtils.getFileExtension(imagePath));
    }

    public static boolean isNonThemed(String extension) {
        return "svg".equalsIgnoreCase(extension) || "bmp".equalsIgnoreCase(extension) || "webp".equalsIgnoreCase(extension);
    }

//    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
//        images = new HashMap<>();
//        imagePath = (String) s.readObject();
//        int size = s.readInt();
//        for (int i = 0; i < size; i++) {
//            if (s.readBoolean()) {
//                ColorTheme colorTheme = (ColorTheme) s.readObject();
//
//                int w = s.readInt();
//                int h = s.readInt();
//                int[] pixels = (int[]) (s.readObject());
//
//                if (pixels != null) {
//                    Toolkit tk = Toolkit.getDefaultToolkit();
//                    ColorModel cm = ColorModel.getRGBdefault();
//                    images.put(colorTheme, new ImageIcon(tk.createImage(new MemoryImageSource(w, h, cm, pixels, 0, w))));
//                }
//            }
//        }
//    }
//
//    private void writeObject(ObjectOutputStream s) throws IOException {
//        s.writeObject(imagePath);
//        s.writeInt(images.size());
//        for (Map.Entry<ColorTheme, ImageIcon> imagesEntry : images.entrySet()) {
//            ImageIcon image = imagesEntry.getValue();
//            s.writeBoolean(image != null);
//            if (image != null) {
//                s.writeObject(imagesEntry.getKey());
//
//                int w = image.getIconWidth();
//                int h = image.getIconHeight();
//                int[] pixels = image.getImage() != null ? new int[w * h] : null;
//
//                if (image.getImage() != null) {
//                    try {
//                        PixelGrabber pg = new PixelGrabber(image.getImage(), 0, 0, w, h, pixels, 0, w);
//                        pg.grabPixels();
//                        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
//                            throw new IOException("failed to load image contents");
//                        }
//                    } catch (InterruptedException e) {
//                        throw new IOException("image load interrupted");
//                    }
//                }
//
//                s.writeInt(w);
//                s.writeInt(h);
//                s.writeObject(pixels);
//            }
//        }
//    }
}
