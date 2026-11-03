package lsfusion.server.base;

import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.interop.base.view.ColorTheme;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AppServerImageTest {

    // an external image keeps its URL in both image and resource results, without fetching bytes or theme variants
    @Test
    public void anExternalImageKeepsItsUrl() throws Exception {
        for (String url : new String[] {"hTtPs://example.invalid/image.svg?a=1;b=2#image",
                "HtTp://example.invalid/image.png;a=1?b=2#image"}) {
            AppServerImage image = AppServerImage.createActionImage(url).get(null);
            AppImage value = IOUtils.deserializeAppImage(IOUtils.serializeAppImage(image.getAppImage()));
            assertEquals(url, value.getImagePath());
            assertNull(value.fontClasses);
            for (ColorTheme theme : ColorTheme.values())
                assertNull(value.getImage(theme));
            assertEquals(url, AppServerImage.convertFileValue(image, false));
        }
    }
}
