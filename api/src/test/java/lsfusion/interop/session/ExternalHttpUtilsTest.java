package lsfusion.interop.session;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

// the Expires of a cookie an action sets, as the web client passes it on, and of one an EXTERNAL HTTP call sends : read in
// any of its forms whatever the language, and turned into the seconds left (#2026: it was read in the language of the JVM,
// only in one form, and turned into milliseconds)
public class ExternalHttpUtilsTest {

    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DASHED_DATE = DateTimeFormatter.ofPattern("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneOffset.UTC);
    private static final int DAY = 24 * 60 * 60;

    private Locale defaultLocale;

    @Before
    public void setRussian() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("ru"));
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    private static Cookie expiring(Instant expires, DateTimeFormatter format) {
        return ExternalHttpUtils.parseCookie("session", "abc; Expires=" + format.format(expires) + "; Path=/");
    }

    // a few seconds pass between the date and its reading
    private static void assertAbout(int expected, int maxAge) {
        Assert.assertTrue("max age " + maxAge + ", expected about " + expected, maxAge <= expected && maxAge > expected - 60);
    }

    @Test
    public void expiresIsTheSecondsLeft() {
        Instant inADay = Instant.now().plus(1, ChronoUnit.DAYS);
        assertAbout(DAY, expiring(inADay, HTTP_DATE).getMaxAge());
        assertAbout(DAY, expiring(inADay, DASHED_DATE).getMaxAge());
        assertAbout(60 * DAY, expiring(Instant.now().plus(60, ChronoUnit.DAYS), HTTP_DATE).getMaxAge());
    }

    @Test
    public void expiresInThePastDeletesTheCookie() {
        Assert.assertEquals(0, expiring(Instant.now().minus(1, ChronoUnit.DAYS), HTTP_DATE).getMaxAge());
    }

    @Test
    public void aFarExpiresKeepsTheCookieAsLongAsMaxAgeGoes() {
        Assert.assertEquals(Integer.MAX_VALUE, ExternalHttpUtils.parseCookie("session", "abc; Expires=Fri, 31 Dec 9999 23:59:59 GMT").getMaxAge());
    }

    @Test
    public void anExpiresThatIsNoDateIsLeftOut() {
        Cookie cookie = ExternalHttpUtils.parseCookie("session", "abc; Expires=never; Path=/");
        Assert.assertEquals(-1, cookie.getMaxAge());
        Assert.assertEquals("/", cookie.getPath());
    }

    // the cookies an EXTERNAL HTTP call sends
    @Test
    public void aSentCookieExpiresAtItsDate() {
        Instant expires = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Assert.assertEquals(expires, ExternalHttpUtils.parseACookie("session", "abc; Expires=" + HTTP_DATE.format(expires)).getExpiryInstant());
    }

    // its Max-Age is in seconds too, which it was taken for milliseconds of
    @Test
    public void aSentCookieLivesItsMaxAgeInSeconds() {
        Instant expiry = ExternalHttpUtils.parseACookie("session", "abc; Max-Age=" + DAY).getExpiryInstant();
        assertAbout(DAY, (int) Duration.between(Instant.now(), expiry).getSeconds());
    }
}
