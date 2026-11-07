package lsfusion.tests;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// the layout of the navigator's windows: their sizes, which the client keeps in the browser's local storage when the page
// is left, and Service's reset of the layout (UiWindowsLayout.lsf)
public class UiWindowsLayoutIT extends UiTest {

    // UiFormsTabsIT's control: the standard tabs hide an inactive tab's close button with the user's setting on - of a
    // form a React view draws too -, by the class the window is given. The setting is the suite's user's, so it is
    // turned off again whatever happens
    @Test
    public void theStandardTabsHideAnInactiveTabsCloseButton() {
        trackRequests();
        String mode = pinMode();
        try {
            pinMode(ALL_PINNED);
            page.waitForResponse(response -> carries(response.request().postData(), "ExecuteNavigatorAction"),
                    () -> open("UiWindowsLayout.uiWindowsLayoutTabs", "UiWindowsLayout.uiWindowsLayoutHideInactiveClose")).finished();
            settled();
            Locator react = open("UiWindowsLayout.uiWindowsLayoutTabs", "UiWindowsLayout.uiWindowsLayoutReact");
            assertThat(react.locator(".ui-windows-layout-react")).isVisible();
            assertThat(open("UiWindowsLayout.uiWindowsLayout", "UiWindowsLayout.uiWindowsLayoutReset")).isVisible();
            settled();

            Locator inactive = page.locator(".tab-bar .nav-item")
                    .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^React layout tab$")));
            assertThat(inactive).not().containsClass("active");
            assertThat(inactive.locator(".tab-close-button")).hasCSS("display", "none");
            assertThat(page.locator(".dont-show-close-button")).hasCount(1);
        } finally {
            try {
                page.waitForResponse(response -> carries(response.request().postData(), "ExecuteNavigatorAction"),
                        () -> open("UiWindowsLayout.uiWindowsLayoutTabs", "UiWindowsLayout.uiWindowsLayoutShowInactiveClose")).finished();
                settled();
            } finally {
                pinMode(mode);
            }
        }
    }

    // an unpinned toolbar is laid out by the standard layout's popup rules
    @Test
    public void anUnpinnedToolbarKeepsItsPopupClass() {
        assertThat(page.locator("[lsfusion-container='System.toolbar']")).containsClass("navbar-popup-over-selected-hover");
    }

    // with the navigator unpinned (the server's default pins the bar) System.root shows its captions only on hover; it is
    // alone with the class in the horizontal strip, so BaseImage leaves it on the window's own view rather than lifting it
    // to the FlexPanel of logo, root and system
    @Test
    public void anUnpinnedRootKeepsItsTextOnHoverClass() {
        String mode = pinMode();
        try {
            pinMode(UNPINNED);
            assertThat(page.locator("[lsfusion-container='System.root']")).containsClass("navbar-text-on-hover");
        } finally {
            pinMode(mode);
        }
    }

    // UiNavigatorButtonsIT's control: System.root drawn by the standard toolbar shows the captions of an unpinned
    // navigator only on hover while a form is open
    @Test
    public void anUnpinnedRootShowsItsCaptionsOnlyOnHover() {
        String mode = pinMode();
        try {
            pinMode(UNPINNED);
            Locator root = page.locator("[lsfusion-container='System.root']");
            assertThat(open("UiWindowsLayout.uiWindowsLayout", "UiWindowsLayout.uiWindowsLayoutReset")).isVisible();
            navigatorSettled(root);
            Locator button = root.locator("[lsfusion-container='UiWindowsLayout.uiWindowsLayout']");
            assertEquals(true, button.evaluate("button => !!button.closest('.navbar-text-on-hover')"));
            caption(button, false);

            root.hover();
            caption(button, true);
        } finally {
            pinMode(mode);
        }
    }

    // UiNavigatorButtonsIT's control: the standard toolbar indents its tree by depth - Administration's sections are
    // drawn there, their items a level deeper
    @Test
    public void theToolbarIndentsTheLevelsOfItsTree() {
        trackRequests();
        page.locator("[lsfusion-container='System.system'] [lsfusion-container='System.administration']").click();
        settled();
        Locator toolbar = page.locator("[lsfusion-container='System.toolbar']");
        Locator application = toolbar.locator("[lsfusion-container='System.application']");
        assertThat(application).containsClass("nav-link-horz-0");
        assertThat(application).hasCSS("margin-left", "0px");
        Locator options = toolbar.locator("[lsfusion-container='System.options']");
        assertThat(options).containsClass("nav-link-horz-1");
        assertThat(options).hasCSS("margin-left", "16px");
    }

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

    // the reset puts back the visibility the windows are declared with, and the navigator's rule over it, as the desktop
    // client does: a navigator window with nothing to draw stays hidden - here System.toolbar, while the section
    // selected has its item drawn by System.system -, not shown empty until the navigator's next update
    @Test
    public void aResetLeavesAWindowWithNothingToDrawHidden() {
        Locator toolbar = page.locator("[lsfusion-container='System.toolbar']");
        assertEquals(true, shown(toolbar));
        Locator form = open("UiWindowsLayout.uiWindowsLayout", "UiWindowsLayout.uiWindowsLayoutReset");
        assertEquals(false, shown(toolbar));

        // the reset comes with the answer to the button's request, and is done once that answer is applied
        page.waitForResponse(response -> carries(response.request().postData(), "ExecuteEventAction"),
                () -> action(form, "resetWindowsLayout").click()).finished();
        page.evaluate("() => new Promise(resolve => requestAnimationFrame(resolve))");
        assertEquals(false, shown(toolbar));
    }

    // whether the layout shows a window: it hides one with display none - an unpinned toolbar, slid out of the viewport,
    // is still shown
    private static boolean shown(Locator window) {
        return !"none".equals(window.evaluate("element => element.style.display"));
    }
}
