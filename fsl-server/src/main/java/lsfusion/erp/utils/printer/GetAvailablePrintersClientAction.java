package lsfusion.erp.utils.printer;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.IOException;


public class GetAvailablePrintersClientAction implements ClientAction {

    public GetAvailablePrintersClientAction() {
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(flavor, attributeSet);
        String printerNames = "";
        for (PrintService printService : printServices) {
            printerNames += printService.getName() + '\n';
        }
        return printerNames;
    }
}
