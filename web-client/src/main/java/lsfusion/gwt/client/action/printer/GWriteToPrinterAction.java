package lsfusion.gwt.client.action.printer;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GWriteToPrinterAction implements GAction {
    public String text;
    public String charset;
    public String printerName;

    public GWriteToPrinterAction() {}

    public GWriteToPrinterAction(String text, String charset, String printerName) {
        this.text = text;
        this.charset = charset;
        this.printerName = printerName;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}