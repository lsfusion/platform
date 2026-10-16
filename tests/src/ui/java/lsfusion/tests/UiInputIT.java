package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

// what is typed into the input INPUT opens comes back to the action that asked for it
public class UiInputIT extends UiTest {

    @Test
    public void typeAnswer() throws Exception {
        Locator form = open("UiInput.uiInput", "UiInput.uiInputDialog");
        Locator ask = form.locator("[lsfusion-container='PROPERTY(uiInputAsk())']");
        ask.click();
        // the input opens in place of the button with the response to the click. Playwright waits for the glass of a
        // request before a click but not before keys, which the glass drops, so they go into the input, not to the page
        Locator input = ask.locator("input");
        String answer = "answer " + System.nanoTime(); // unique : the answer of a failed attempt stays in the database for its rerun
        input.fill(answer);
        input.press("Enter");
        check("UiInput.uiInputCheckAnswer", answer);
        close(form);
    }
}
