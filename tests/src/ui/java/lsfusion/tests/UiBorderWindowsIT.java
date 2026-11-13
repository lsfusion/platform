package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ViewportSize;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// windows placed by an edge of the page - LEFT, RIGHT, TOP or BOTTOM - rather than by POSITION (src/ui/border:
// UiBorderWindows.lsf). A window is on every page of its application, so the class runs against a test server of its
// own, on the suite's web client
public class UiBorderWindowsIT extends UiTest {

    private static final String DATABASE = "lsfusion_test_ui_border_" + ProcessHandle.current().pid();
    private static Process server;
    private static int rmiPort;

    @BeforeClass
    public static void startBorderServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        rmiPort = ports[1];
        server = startServer(DATABASE, ports, TestServer.BASE.resolve("src/ui/border"));
    }

    @AfterClass
    public static void stopBorderServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    @Override
    protected String main() {
        return "/main?port=" + rmiPort;
    }

    // the web client laid such a window out as one of no size in the top left corner, in the place of System.logo
    // (#1976). It is laid out along its edge, as the desktop client lays it out: the top and the bottom bands across the
    // whole page, the left and the right columns between them and beside the rest - the navigator's top strip among
    // it -, each as big as what it draws. Of two windows by one edge, the one declared first is by the edge itself
    @Test
    public void aWindowPlacedByAnEdgeIsLaidOutAlongIt() {
        for (String edge : new String[]{"Top", "Left", "Right", "Bottom", "BottomInner"})
            assertThat(window("UiBorderWindows.uiBorderWindows" + edge).locator("[lsfusion-container='UiBorderWindows.uiBorderWindows" + edge + "Form']")).isVisible();

        ViewportSize viewport = page.viewportSize();
        BoundingBox top = box("UiBorderWindows.uiBorderWindowsTop"), left = box("UiBorderWindows.uiBorderWindowsLeft"),
                right = box("UiBorderWindows.uiBorderWindowsRight"), bottom = box("UiBorderWindows.uiBorderWindowsBottom"),
                inner = box("UiBorderWindows.uiBorderWindowsBottomInner"), root = box("System.root");

        assertEquals("the top band's top edge", 0, top.y, 1);
        assertAcross("the top band", top, viewport);
        assertEquals("the bottom band's lower edge", viewport.height, bottom.y + bottom.height, 1);
        assertAcross("the bottom band", bottom, viewport);
        assertAcross("the inner bottom band", inner, viewport);
        adjacent("the inner bottom band and the bottom band", inner.y + inner.height, bottom.y);

        assertEquals("the left column's left edge", 0, left.x, 1);
        assertEquals("the right column's right edge", viewport.width, right.x + right.width, 1);
        for (BoundingBox column : new BoundingBox[]{left, right}) {
            adjacent("the top band and a column", top.y + top.height, column.y);
            adjacent("a column and the inner bottom band", column.y + column.height, inner.y);
            assertTrue("a column is " + column.width + " px wide", column.width > 0 && column.width < 400);
        }

        assertTrue("System.root starts at " + root.x + ", the left column ends at " + (left.x + left.width), root.x >= left.x + left.width);
        assertTrue("System.root ends at " + (root.x + root.width) + ", the right column starts at " + right.x, root.x + root.width <= right.x);
        assertTrue("System.root starts at " + root.y + ", the top band ends at " + (top.y + top.height), root.y >= top.y + top.height);
    }

    // a band across the whole page, as high as what it draws
    private static void assertAcross(String band, BoundingBox box, ViewportSize viewport) {
        assertEquals(band + "'s left edge", 0, box.x, 1);
        assertEquals(band + "'s width", viewport.width, box.width, 1);
        assertTrue(band + " is " + box.height + " px high", box.height > 0 && box.height < 100);
    }

    // the second starts where the first ends, but for the gap a split leaves between its parts
    private static void adjacent(String what, double end, double start) {
        double gap = start - end;
        assertTrue(what + " are " + gap + " px apart", gap >= 0 && gap < 10);
    }

    private BoundingBox box(String window) {
        return window(window).boundingBox();
    }

    private Locator window(String name) {
        return page.locator("[lsfusion-container='" + name + "']");
    }
}
