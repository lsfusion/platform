package lsfusion.gwt.client.base.view;

import com.bfr.client.selection.Range;
import com.bfr.client.selection.RangeEndPoint;
import com.bfr.client.selection.Selection;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;

import java.util.function.Consumer;

public class CopyPasteUtils {
    private static final Selection selection = Selection.getSelection();

    // actually it justs sets selection, since we don't consume event default handler does the rest
    public static void putIntoClipboard(Element element) {
        if (element != null && (selection.getRange() == null || selection.getRange().getText().isEmpty())) {
            Range range = new Range(element);
            selection.setRange(range);
            setClipboardData(selection.getRange().getText());
            //DELAY
            Scheduler.get().scheduleFixedDelay(() -> {
                range.collapse(true);
                selection.setRange(range);
                return false;
            }, 100);
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
        $doc.execCommand('copy');
    }-*/;

    // copy from range, but we need stopper if in element there is no text, but a lot of elements (for example grid with only actions - images)
    public static Text getAdjacentTextElement(Node current,
                                              Node topMostNode,
                                              boolean forward,
                                              boolean traversingUp,
                                              Result<Integer> depth) {
        depth.set(depth.result + 1);
        if(depth.result > 10000)
            return null;

        Text res = null;
        Node node;

        // If traversingUp, then the children have already been processed
        if (!traversingUp) {
            if (current.getChildCount() > 0) {
                node = forward ? current.getFirstChild()
                        : current.getLastChild();

                if (node.getNodeType() == Node.TEXT_NODE) {
                    res = (Text) node;
                } else {
                    // Depth first traversal, the recursive call deals with
                    // siblings
                    res = getAdjacentTextElement(node, topMostNode,
                            forward, false, depth);
                }
            }
        }

        if (res == null) {
            node = forward ? current.getNextSibling()
                    : current.getPreviousSibling();
            // Traverse siblings
            if (node != null) {
                if (node.getNodeType() == Node.TEXT_NODE) {
                    res = (Text) node;
                } else {
                    // Depth first traversal, the recursive call deals with
                    // siblings
                    res = getAdjacentTextElement(node, topMostNode,
                            forward, false, depth);
                }
            }
        }

        // Go up and over if still not found
        if ((res == null) && (current != topMostNode)) {
            node = current.getParentNode();
            // Stop at document (technically could stop at "html" tag)
            if ((node != null) && (node.getNodeType() != Node.DOCUMENT_NODE)) {
                res = getAdjacentTextElement(node, topMostNode,
                        forward, true, depth);
            }
        }
        return res;
    }

    public static void setEmptySelection(final Element element) {
        Node textNode;
        // just putting empty selection to any text containing element
        if (element != null && !GwtClientUtils.isIEUserAgent() && (textNode = getAdjacentTextElement(element, element, true, false, new Result<>(0))) != null) {
            Element textElement;
            textElement = GwtClientUtils.getElement(textNode);
            if(textElement == null) // if we haven't found element, just put it somewhere
                textElement = element;
            selection.setRange(new Range(new RangeEndPoint(textElement, true)));
        }
    }

    private static void consumeLine(String line, Consumer<String> paste) {
        line = line.replaceAll("\r\n", "\n");
        paste.accept(line);
    }

    public static native String getFromClipboard(EventHandler handler, Consumer<String> paste)
    /*-{
        // assert that event is ONPASTE (only in this case clipboardData will be filled)
        @CopyPasteUtils::getFromClipboardEvent(*)(handler, paste);

        // this approach is not secure (however is more flexible and reliable) and requires secure origin
        // so for now will use ONPASTE event
//        var clipboard = $wnd.navigator.clipboard;
//        if(clipboard != null) // this feature is unavailable for example when origin is not secure (not HTTPS or localHost)
//            clipboard.readText().then(function (cliptext) {
//                @CopyPasteUtils::consumeLine(*)(cliptext, paste);
//            });
    }-*/;

    public static void getFromClipboardEvent(EventHandler handler, Consumer<String> paste) {
        String line = CopyPasteUtils.getEventClipboardData(handler.event);
        if (!line.isEmpty()) {
            handler.consume();
            line = line.replaceAll("\r\n", "\n");
            paste.accept(line);
        }
    }

    public static native String getEventClipboardData(Event event)
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
