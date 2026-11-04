package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// a file a React view is given is a link to it (UiReactFile.lsf, its view in UiReactFile.js), whatever the server sends
// it as: the value of a PDF, of a video and of an image and the footer of an image as an image made of the file, the
// value of a file of another class (a text), its footer and what a call to the server returns as the file itself, an
// image's too
public class UiReactFileIT extends UiTest {

    @Test
    public void aFileAViewIsGivenIsALinkToIt() throws Exception {
        Locator form = open("UiReactFile.uiReactFile", "UiReactFile.uiRfForm");
        page.waitForFunction("() => window.uiRfData && uiRfData.uiRfPdf.value && uiRfData.r?.uiRfFile?.footer && uiRfData.r.uiRfPicture?.footer");
        assertEquals("a PDF", fetch("uiRfData.uiRfPdf.value"));
        assertEquals("a video", fetch("uiRfData.uiRfVideo.value"));
        assertEquals("an image", fetch("uiRfData.uiRfImage.value"));
        assertEquals("a text", fetch("uiRfData.uiRfText.value"));
        assertEquals("a footer", fetch("uiRfData.r.uiRfFile.footer"));
        assertEquals("an image footer", fetch("uiRfData.r.uiRfPicture.footer"));
        assertEquals("a PDF", fetch("uiRfController.form.exec('uiRfPdfResult')"));
        assertEquals("an image", fetch("uiRfController.form.exec('uiRfImageResult')"));
        // a property named image is given as the image its string names - its HTML, as a classic custom view is
        assertEquals("<i class=\"bi bi-plus wrap-text-img\"></i>", page.evaluate("uiRfData.image.value"));
        close(form);
    }

    // a footer of another type than its column's is given as a value of its own type: a string footer under a column of
    // files was converted as a file, and the view was given a link to a file of that name
    @Test
    public void aFooterIsGivenAsAValueOfItsOwnType() {
        Locator form = open("UiReactFile.uiReactFile", "UiReactFile.uiRfForm");
        page.waitForFunction("() => window.uiRfData && uiRfData.r?.uiRfFileTotal?.footer");
        assertEquals("a string footer", page.evaluate("uiRfData.r.uiRfFileTotal.footer"));
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
