package lsfusion.client.base.view;

import lsfusion.base.file.AppImage;
import lsfusion.interop.base.view.ColorTheme;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class ClientImagesTest {
    // URL images have no bytes: the desktop client must not substitute its unknown-image icon
    @Test
    public void anExternalImageHasNoDesktopIcon() {
        for (String url : new String[]{"http://example.invalid/image.png", "hTtPs://example.invalid/image.svg"}) {
            AppImage image = new AppImage(null, url, null);
            for (ColorTheme colorTheme : ColorTheme.values())
                assertNull(ClientImages.getImage(image, colorTheme));
        }
    }
}
