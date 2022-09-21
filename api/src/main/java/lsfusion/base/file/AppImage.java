package lsfusion.base.file;

import lsfusion.base.BaseUtils;
import lsfusion.base.ResourceUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.interop.base.view.ColorTheme;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AppImage implements Serializable {
    public static final long serialVersionUID = 42L;

    public AppImage() {
    }

    private Map<ColorTheme, String> imagePathes;
    private Map<ColorTheme, RawFileData> images;

    public String imagePath;
    public String fontClasses;

    private static final boolean useFA = false;

    private static MAddExclMap<String, String> predefinedFontClasses = MapFact.mAddExclMap();
    static {
        predefinedFontClasses.exclAdd("apply.png", "bi bi-save");
        predefinedFontClasses.exclAdd("cancel.png", "bi bi-archive");
        predefinedFontClasses.exclAdd("ok.png", "bi bi-check");
        predefinedFontClasses.exclAdd("close.png", "bi bi-x");
        predefinedFontClasses.exclAdd("editReport.png", "bi bi-pencil-square");
        predefinedFontClasses.exclAdd("refresh.png", "bi bi-arrow-repeat");

        predefinedFontClasses.exclAdd("add.png", "fa-solid fa-plus");
        predefinedFontClasses.exclAdd("edit.png", "fa-solid fa-pen");
        predefinedFontClasses.exclAdd("delete.png", "fa-solid fa-minus");

        predefinedFontClasses.exclAdd("email.png", "fa-regular fa-envelope");

        predefinedFontClasses.exclAdd("dialog.png", "fa-solid fa-ellipsis");
        predefinedFontClasses.exclAdd("reset.png", "fa-solid fa-xmark");
    }

    public AppImage(String imagePath) {
        Map<ColorTheme, String> imagePathes = new HashMap<>();
        Map<ColorTheme, RawFileData> images = new HashMap<>();

        for (ColorTheme colorTheme : ColorTheme.values()) {
            Result<String> fullPath = new Result<>();
            boolean defaultTheme = colorTheme.isDefault();
            RawFileData themedImageFile = ResourceUtils.findResourceAsFileData(colorTheme.getImagePath(imagePath), !defaultTheme, false, fullPath, "images");
            if(defaultTheme || themedImageFile != null) {
                themedImageFile.getID(); // to calculate the cache
                images.put(colorTheme, themedImageFile);
                imagePathes.put(colorTheme, fullPath.result);
            }
        }

        this.imagePathes = imagePathes;
        this.images = images;

        this.fontClasses = predefinedFontClasses.get(BaseUtils.getFileNameAndExtension(imagePath));
    }

    public transient Object desktopClientImages;

    public RawFileData getImage(ColorTheme colorTheme) {
        return images.get(colorTheme);
    }

    public String getImagePath(ColorTheme colorTheme) {
        return imagePathes.get(colorTheme);
    }
    
    public boolean isGif(ColorTheme colorTheme) {
        return "gif".equalsIgnoreCase(BaseUtils.getFileExtension(getImagePath(colorTheme)));
    }

    // constants

    public static final AppImage FORMTOP = new AppImage("formTop.png");
    public static final AppImage FORM = new AppImage("form.png");

    public static final AppImage ACTIONTOP = new AppImage("actionTop.png");
    public static final AppImage ACTION = new AppImage("action.png");

    public static final AppImage OPENTOP = new AppImage("openTop.png");
    public static final AppImage OPEN = new AppImage("open.png");

    public static final AppImage ADD = new AppImage("add.png");
    public static final AppImage EDIT = new AppImage( "edit.png");
    public static final AppImage DELETE = new AppImage( "delete.png");

    public static final AppImage EMAIL = new AppImage( "email.png");

    public static final AppImage DIALOG = new AppImage( "dialog.png");
    public static final AppImage RESET = new AppImage( "reset.png");

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
