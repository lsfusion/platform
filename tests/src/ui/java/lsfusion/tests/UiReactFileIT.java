package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// a file a React view is given is a link to it (UiReactFile.lsf, its view in UiReactFile.js), whatever the server sends
// it as: the value of a PDF and of a video as an image made of the file, as an image's, and what a call to the server
// returns as the file itself, an image's too
public class UiReactFileIT extends UiTest {

    @Test
    public void aFileAViewIsGivenIsALinkToIt() throws Exception {
        Locator form = open("UiReactFile.uiReactFile", "UiReactFile.uiRfForm");
        page.waitForFunction("() => window.uiRfData && uiRfData.uiRfPdf.value");
        assertEquals("a PDF", fetch("uiRfData.uiRfPdf.value"));
        assertEquals("a video", fetch("uiRfData.uiRfVideo.value"));
        assertEquals("an image", fetch("uiRfData.uiRfImage.value"));
        assertEquals("a PDF", fetch("uiRfController.exec('uiRfPdfResult')"));
        assertEquals("an image", fetch("uiRfController.exec('uiRfImageResult')"));
        close(form);
    }

    // what the link an expression gives - awaited, 10 s at most - points at: its text, or the status it is refused with
    private String fetch(String link) {
        return (String) page.evaluate("async () => {"
                + " const link = await Promise.race([" + link + ","
                + "     new Promise((resolve, reject) => setTimeout(() => reject(new Error('no answer in 10 s')), 10000))]);"
                + " const response = await fetch(link);"
                + " return response.ok ? response.text() : link + ' -> HTTP ' + response.status; }");
    }
}
