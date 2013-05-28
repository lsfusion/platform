package platform.gwt.form.client.form.ui;

import com.bfr.client.selection.Range;
import com.bfr.client.selection.RangeEndPoint;
import com.bfr.client.selection.Selection;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import platform.gwt.base.client.GwtClientUtils;

public class CopyPasteUtils {
    private static Selection selection = Selection.getSelection();

    public static void putIntoClipboard(Element element) {
        if (selection.getRange() == null || selection.getRange().getText().isEmpty()) {
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

    public static void setEmptySelection(Element element) {
        if (!GwtClientUtils.isIEUserAgent()) {
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
