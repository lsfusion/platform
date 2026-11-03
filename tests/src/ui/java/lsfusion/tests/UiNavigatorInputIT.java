package lsfusion.tests;

import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// An INPUT asked by a navigator action or the navigator controller's exec() is cancelled as a user cancels one:
// there is no cell to enter it in; ELSE runs, with no error dialog.
public class UiNavigatorInputIT extends UiTest {

    @Test
    public void aNavigatorActionCancelsInput() throws Exception {
        String attempt = "navigator " + System.nanoTime();
        exec("UiNavigatorInput.uiNavigatorInputSeed", attempt);
        open("UiNavigatorInput.uiNavigatorInput", "UiNavigatorInput.uiNavigatorInputAsk");
        check("UiNavigatorInput.uiNavigatorInputCheck", attempt);
        assertThat(page.locator(".modal-content")).hasCount(0);
    }

    @Test
    public void aNavigatorControllerExecCancelsInput() throws Exception {
        String attempt = "controller " + System.nanoTime();
        exec("UiNavigatorInput.uiNavigatorInputSeed", attempt);
        open("UiNavigatorInput.uiNavigatorInput", "UiNavigatorInput.uiNavigatorInputThroughController");
        check("UiNavigatorInput.uiNavigatorInputCheck", attempt);
        assertThat(page.locator(".modal-content")).hasCount(0);
    }
}
