package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.base.file.RawFileData;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.ImagePropertyRenderer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ImageLinkPropertyRenderer extends LinkPropertyRenderer {
    private ImageIcon icon;

    public static Map<ClientPropertyDraw, Map<String, RawFileData>> imageCache = new WeakHashMap<>();

    public ImageLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        RawFileData iconBytes = readImage(property, (String) value);
        
        icon = null; // сбрасываем
        if (iconBytes != null) {
            Image image = ImagePropertyRenderer.convertValue(iconBytes);
            if (image != null) {
                icon = new ImageIcon(image);
            }
        }
        super.setValue(value);
    }

    @Override
    protected ImageIcon getImageIcon() {
        return icon;
    }

    @Override
    public void paintLabelComponent(Graphics g) {
        super.paintLabelComponent(g);
        
        if (icon != null) {
            ImagePropertyRenderer.paintComponent(getComponent(), g, icon, property);
        }
    }

    public static synchronized void clearChache(ClientPropertyDraw property) {
        imageCache.remove(property);
    }
    
    private static synchronized RawFileData getFromCache(ClientPropertyDraw property, String url) {
        Map<String, RawFileData> imageMap = imageCache.get(property);
        return imageMap == null ? null : imageMap.get(url);
    }
    
    private static synchronized boolean isCached(ClientPropertyDraw property, String url) {
        Map<String, RawFileData> imageMap = imageCache.get(property);
        return imageMap != null && imageMap.containsKey(url);
    }
    
    private static synchronized void putIntoCache(ClientPropertyDraw property, String url, RawFileData image) {
        Map<String, RawFileData> imageMap = imageCache.get(property);
        if (imageMap == null) {
            imageMap = new HashMap<>();
            imageCache.put(property, imageMap);
        }
        imageMap.put(url, image);
    }

    public static RawFileData readImage(ClientPropertyDraw property, String link) {
        try {
            RawFileData result = getFromCache(property, link);
            if (result == null && !isCached(property, link)) {
                URLConnection httpcon = new URL(link).openConnection();
                httpcon.addRequestProperty("User-Agent", "");
                InputStream inputStream = httpcon.getInputStream();
                result = new RawFileData(inputStream);
                
                ImageIcon icon = new ImageIcon(result.getBytes()); // проверка на то, что массив байтов - картинка. readBytesFromStream возвращает 4 байта, а не null
                if (icon.getIconWidth() < 0 || icon.getIconHeight() < 0) {
                    result = null;
                }
                
                putIntoCache(property, link, result);
            }
            return result;
        } catch (IOException e) {
            putIntoCache(property, link, null);
            return null;
        }
    }
}