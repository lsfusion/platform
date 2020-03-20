package lsfusion.client.base.view;

import lsfusion.client.controller.MainController;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.ResourceUtils.readImage;

public class ClientImages {
    private static Map<String, ImageIcon> images = new HashMap<>();
    
    public static ImageIcon get(String path) {
        ImageIcon image = images.get(path);
        if (image == null) {
            ImageIcon newImage = readImage(MainController.colorTheme.getImagePath(path));
            if (newImage != null) {
                images.put(path, newImage);
            } else {
                images.put(path, readImage(path)); // default color theme
            }
        }
        return image;
    }
    
    public static void reset() {
        images.clear();
    } 
}
