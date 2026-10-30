package lsfusion.tests;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Route;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

// the sizes of the navigator's windows, which the client keeps in the browser's local storage when the page is left
public class UiWindowsLayoutIT extends UiTest {

    // the sizes are stored when the page is left in a browser that does not fire unload: Chrome stops firing it for a
    // growing share of its users and, from version 158, for all of them, so a panel collapsed by its splitter came back
    // at its default size on every login (#1945). The Permissions-Policy header turns unload off in the Chrome of any version
    @Test
    public void theSizesAreStoredWhenThePageIsLeftWithoutUnload() {
        page.route("**/main", route -> {
            APIResponse response = route.fetch();
            Map<String, String> headers = new HashMap<>(response.headers());
            headers.put("permissions-policy", "unload=()");
            route.fulfill(new Route.FulfillOptions().setResponse(response).setHeaders(headers));
        });
        page.reload(); // the document the rule opened was served without the header
        page.locator("[lsfusion-container='System.root']").waitFor();
        assertEquals(false, page.evaluate("() => document.featurePolicy.allowsFeature('unload')"));
        page.evaluate("() => localStorage.clear()"); // what leaving the document without the header stored

        page.reload();
        page.locator("[lsfusion-container='System.root']").waitFor();
        assertEquals(true, page.evaluate("() => Object.keys(localStorage).some(key => key.endsWith('_System.log'))"));
    }
}
