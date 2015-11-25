package lsfusion.gwt.base.client.ui;

import com.bfr.client.selection.Range;
import com.bfr.client.selection.RangeEndPoint;
import com.bfr.client.selection.Selection;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.base.client.GwtClientUtils;

public class CopyPasteUtils {
    private static Selection selection = Selection.getSelection();

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
                setClipboardData(rangeText);    
            }
        }
    }

    public static native void setClipboardData(String text)
    /*-{
        $wnd.clipboardData.setData("text/plain", text); // в Firefox не работает
    }-*/;

    public static void setEmptySelection(final Element element) {
        if (element != null && !GwtClientUtils.isIEUserAgent() && Range.getAdjacentTextElement(element, element, true, false) != null) {
            // для вставки в Chrome без предварительного клика по ячейке, но валит весь селекшн в IE
            selection.setRange(new Range(new RangeEndPoint(element, true)));
        }
    }

    public static native String getClipboardData(Event event)
    /*-{
        var text = "";

        // This should eventually work in Firefox:
        // https://bugzilla.mozilla.org/show_bug.cgi?id=407983
        if (event.clipboardData) // WebKit (Chrome/Safari)
        {
            try {
                text = event.clipboardData.getData("text/plain");
                return text;
            }
            catch (e) {
            }
        }

        if ($wnd.clipboardData) // IE
        {
            try {
                text = $wnd.clipboardData.getData("Text");
                return text;
            }
            catch (e) {
            }
        }

        return text;
    }-*/;
}
