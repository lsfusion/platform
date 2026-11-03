package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;


// a property named image under a React view (UiReactImage.lsf, its view in UiReactImage.js)
public class UiReactImageIT extends UiTest {

    // a string footer stays text, even though the property's cells are images
    @Test
    public void aStringFooterIsText() throws Exception {
        exec("UiReactImage.uiRiSeed");
        Locator form = open("UiReactImage.uiRiFolder", "UiReactImage.uiRiFooter");
        view.waitFor("data.r.image.footer === 'footer text'");
        close(form);
    }

    private final View view = view("window.uiRiView", "");
}
