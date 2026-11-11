package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ScrollToEnd;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.order.Scroll;
import lsfusion.interop.form.remote.BatchRequest;

public class ScrollToEndHandler extends BatchRequestHandler<ScrollToEnd> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ScrollToEndHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(ScrollToEnd action) {
        Scroll scrollType = action.toEnd ? Scroll.END : Scroll.HOME;
        return new BatchRequest.ScrollGroupObject(action.groupId, scrollType, gwtConverter.convertOrCast(action.changeSelection));
    }
}
