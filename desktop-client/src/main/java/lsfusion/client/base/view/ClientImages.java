package lsfusion.client.base.view;

import lsfusion.base.file.RawFileData;
import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.client.controller.MainController;
import lsfusion.interop.base.view.ColorTheme;

import javax.swing.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.interop.base.view.ColorTheme.DEFAULT;

public class ClientImages {
    private static Map<String, ImageIcon> images = new HashMap<>();
    
    public static ImageIcon get(String path) {
        ImageIcon image = images.get(path);
        if (image == null) {
            image = readImage(MainController.colorTheme.getImagePath(path));
            if (image != null) {
                images.put(path, image);
            } else {
                ImageIcon defaultThemeImage = readImage(path);
                if (defaultThemeImage != null) {
                    image = ClientColorUtils.createFilteredImageIcon(defaultThemeImage);
                    images.put(path, image);
                }
            }
        }
        return image;
    }
    
    public static void reset() {
        images.clear();
    }

    public static ImageIcon getImage(SerializableImageIconHolder imageHolder) {
        return getImage(imageHolder, MainController.colorTheme);
    }

    public static ImageIcon getImage(SerializableImageIconHolder imageHolder, ColorTheme colorTheme) {
        if (imageHolder == null) {
            return null;
        }

        Map<ColorTheme, ImageIcon> desktopClientImages = (Map<ColorTheme, ImageIcon>)imageHolder.desktopClientImages;
        if(desktopClientImages == null) {
            desktopClientImages = new HashMap<>();
            imageHolder.desktopClientImages = desktopClientImages;
        }

        ImageIcon themeImageIcon = desktopClientImages.get(colorTheme);
        if(themeImageIcon == null) {
            themeImageIcon = calculateImage(imageHolder, colorTheme);
            desktopClientImages.put(colorTheme, themeImageIcon);
        }
        return themeImageIcon;
    }

    public static ImageIcon calculateImage(SerializableImageIconHolder imageHolder, ColorTheme colorTheme) {
        RawFileData themeImage = imageHolder.getImage(colorTheme);
        if (themeImage == null)
            return ClientColorUtils.createFilteredImageIcon(getImage(imageHolder, DEFAULT));
        return themeImage.getImageIcon();
    }

    public static ImageIcon readImage(String imagePath) {
        URL resource = ClientImages.class.getResource("/images/" + imagePath);
        return resource != null ? new ImageIcon(resource) : null;
    }
}
