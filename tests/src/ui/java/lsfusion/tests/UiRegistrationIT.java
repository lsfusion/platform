package lsfusion.tests;

import org.junit.Assert;
import org.junit.Test;

// The web client keeps the server's settings, so that its login page asks the server nothing: registration switched on or
// off in the navigator is offered, or no longer offered, by the login page's next draw.
public class UiRegistrationIT extends UiTest {

    @Test
    public void theLoginPageFollowsRegistrationSwitchedInTheNavigator() throws Exception {
        Assert.assertFalse(loginOffersRegistration()); // a new database, and the settings kept since the page was loaded

        open("UiRegistration.uiRegistration", page.locator("[lsfusion-container='UiRegistration.hrEnable']"));
        try {
            loginOffersRegistration(true);
        } finally {
            open("UiRegistration.uiRegistration", page.locator("[lsfusion-container='UiRegistration.hrDisable']"));
        }
        loginOffersRegistration(false);
    }

    // the server applies the change a moment after the click, and the settings are dropped with its answer : the login
    // page is read again for 10 s
    private static void loginOffersRegistration(boolean offers) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (loginOffersRegistration() != offers) {
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("the login page " + (offers ? "does not offer" : "still offers") + " registration");
            Thread.sleep(200);
        }
    }

    // the login page as a browser that has not logged in gets it : its form, and not the page of a failure
    private static boolean loginOffersRegistration() throws Exception {
        String login = webPage("/login");
        if (!login.contains("id=\"login-form\""))
            throw new AssertionError("no login form:\n" + login);
        return login.contains("class=\"reg-block\"");
    }
}
