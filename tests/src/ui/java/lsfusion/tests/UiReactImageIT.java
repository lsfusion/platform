package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// a property named image under a React view (UiReactImage.lsf, its view in UiReactImage.js)
public class UiReactImageIT extends UiTest {

    // a string footer stays text, even though the property's cells are images
    @Test
    public void aStringFooterIsText() throws Exception {
        exec("UiReactImage.uiRiSeed", Long.toString(System.nanoTime()));
        Locator form = open("UiReactImage.uiRiFolder", "UiReactImage.uiRiFooter");
        view.waitFor("data.r.image.footer === 'footer text'");
        close(form);
    }

    // a SELECT named image gives its options to the React view
    @Test
    public void aSelectIsGivenAsOptions() throws Exception {
        String name = Long.toString(System.nanoTime());
        exec("UiReactImage.uiRiSeed", name);
        Locator form = open("UiReactImage.uiRiFolder", "UiReactImage.uiRiSelect");
        view.waitFor("data.r.list.length === 1 && data.r.list[0].image.select.list.some(o => o.selected)");
        assertEquals("New " + name, view.value("data.r.list[0].image.value"));
        assertEquals(true, view.value("data.r.list[0].image.select.list.some(o => o.value === 'Done " + name + "')"));
        close(form);
    }

    // the platform's SELECT named image opens and shows the selected option too
    @Test
    public void aSelectThePlatformDrawsOpens() throws Exception {
        String name = Long.toString(System.nanoTime());
        exec("UiReactImage.uiRiSeed", name);
        Locator form = open("UiReactImage.uiRiFolder", "UiReactImage.uiRiPlatformSelect");
        assertThat(form.locator("tbody select option:checked")).hasText("New " + name);
        close(form);
    }

    private final View view = view("window.uiRiView", "");
}
