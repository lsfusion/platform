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
            image = readImage(MainController.colorTheme.getImagePath(path));
            if (image != null) {
                images.put(path, image);
            } else {
                images.put(path, image = readImage(path)); // default color theme
            }
        }
        return image;
    }
    
    public static void reset() {
        images.clear();
    } 
}
