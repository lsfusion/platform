package lsfusion.client.form.editor.rich;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lsfusion.client.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.ImageView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class CachedImageView extends ImageView {

    private static final File imgCacheFolder = new File(Main.fusionDir, "image_cache");

    private static final LoadingCache<URL, File> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new ImageFileCacheLoader());

    private File imageFile;

    public CachedImageView(Element elem) {
        super(elem);

        try {
            String src = (String) getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
            URL url = new URL(((HTMLDocument)getDocument()).getBase(), src);
            imageFile = cache.get(url);
        } catch (Throwable e) {
            imageFile = null;
        }
        
        setLoadsSynchronously(imageFile != null);
    }

    public URL getImageURL() {
        if (imageFile != null) {
            try {
                return imageFile.toURI().toURL();
            } catch (MalformedURLException ignore) {
            }
        }

        return super.getImageURL();
    }

    private static class ImageFileCacheLoader extends CacheLoader<URL, File> {
        public File load(URL url) throws Exception {
            String path = url.getPath();
            String ext = path.endsWith(".jpg") ||
                             path.endsWith(".JPG") ||
                             path.endsWith(".jpeg") ||
                             path.endsWith(".JPEG") ? "jpg" : "png";

            int hc = path.hashCode();
            File imageFile = new File(imgCacheFolder, hc + "." + ext);
            if (!imageFile.exists()) {
                imageFile.getParentFile().mkdirs();

                ImageIcon icon = new ImageIcon(url);
                ImageIO.write(toBufferedImage(icon), ext, imageFile);
                
                return imageFile;
            }
            return imageFile;
        }

        public BufferedImage toBufferedImage(ImageIcon icon) {
            BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = bi.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return bi;
        }
    }
}
