package lsfusion.gwt.client.base.view;

import com.bfr.client.selection.Range;
import com.bfr.client.selection.RangeEndPoint;
import com.bfr.client.selection.Selection;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;

import java.util.function.Consumer;

public class CopyPasteUtils {
    private static Selection selection = Selection.getSelection();

    // actually it justs sets selection, since we don't consume event default handler does the rest
    public static void putIntoClipboard(Element element) {
        if (element != null && (selection.getRange() == null || selection.getRange().getText().isEmpty())) {
            selection.setRange(new Range(element));

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    Range range = selection.getRange();
                    range.collapse(true);
                    selection.setRange(range);
                }
            });
        }
    }
    
    public static void putSelectionIntoClipboard() {
        Range range = selection.getRange();
        if (range != null) {
            String rangeText = range.getText();
            if (!rangeText.isEmpty()) {
                setClipboardData2(rangeText);    
            }
        }
    }

    public static native void setClipboardData(String text)
    /*-{
        if ($wnd.clipboardData) {
            $wnd.clipboardData.setData("text/plain", text); // в Firefox не работает
        }
    }-*/;

    public static native void setClipboardData2(String text)
    /*-{
        if(clipboard) {
            clipboard.copy(text) // в Firefox не работает
        }
    }-*/;

    public static void setEmptySelection(final Element element) {
        if (element != null && !GwtClientUtils.isIEUserAgent() && Range.getAdjacentTextElement(element, element, true, false) != null) {
            // для вставки в Chrome без предварительного клика по ячейке, но валит весь селекшн в IE
            selection.setRange(new Range(new RangeEndPoint(element, true)));
        }
    }

    private static void consumeLine(String line, Consumer<String> paste) {
        line = line.replaceAll("\r\n", "\n");
        paste.accept(line);
    }

    public static native String getFromClipboard(EventHandler handler, Consumer<String> paste)
    /*-{
        $wnd.navigator.clipboard.readText().then(function (cliptext) {
            @CopyPasteUtils::consumeLine(*)(cliptext, paste);
        });
    }-*/;

//    public static void getFromClipboard(EventHandler handler, Consumer<String> paste) {
//        String line = CopyPasteUtils.getClipboardData(handler.event);
//        if (!line.isEmpty()) {
//            handler.consume();
//            line = line.replaceAll("\r\n", "\n");
//            paste.accept(line);
//        }
//    }

//    public static native String getClipboardData(Event event)
//    /*-{
//        var text = "";
//
//        // This should eventually work in Firefox:
//        // https://bugzilla.mozilla.org/show_bug.cgi?id=407983
//        if (event.clipboardData) // WebKit (Chrome/Safari)
//        {
//            try {
//                text = event.clipboardData.getData("text/plain");
//                return text;
//            }
//            catch (e) {
//            }
//        }
//
//        if ($wnd.clipboardData) // IE
//        {
//            try {
//                text = $wnd.clipboardData.getData("Text");
//                return text;
//            }
//            catch (e) {
//            }
//        }
//
//        return text;
//    }-*/;
}
