package lsfusion.client.base.view;

import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.client.controller.MainController;
import lsfusion.interop.base.view.ColorTheme;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.ResourceUtils.readImage;
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
                ImageIcon defaultThemeImage = readImage(DEFAULT.getImagePath(path));
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
        
        ImageIcon themeImage = imageHolder.getImage(colorTheme);
        if (themeImage == null) {
            ImageIcon imageIcon = ClientColorUtils.createFilteredImageIcon(imageHolder.getImage(DEFAULT));
            imageHolder.putImage(colorTheme, imageIcon);
            return imageIcon;
        }
        return themeImage;
    }

    
}
