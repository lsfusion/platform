package lsfusion.client.base.view;

import lsfusion.client.view.MainFrame;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.ResourceUtils.readImage;

public class ClientImages {
    private static Map<String, ImageIcon> images = new HashMap<>();
    
    public static ImageIcon get(String path) {
        ImageIcon image = images.get(path);
        if (image == null) {
            image = refreshIcon(path);
        }
        return image;
    }
    
    public static void refreshIcons() {
        for (String path : images.keySet()) {
            refreshIcon(path);
        }
    } 

    public static ImageIcon refreshIcon(String path) {
        ImageIcon image = readImage(MainFrame.colorTheme.getImagePath(path));
        if (image == null) {
            image = readImage(path); // default color theme
        }
        images.put(path, image);
        return image;
    }
}
