package lsfusion.server.physics.dev.integration.external.to.equ.printer.client;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaTray;
import java.util.ArrayList;
import java.util.List;


public class GetAvailablePrintersClientAction implements ClientAction {

    public GetAvailablePrintersClientAction() {
    }

    public Object dispatch(ClientActionDispatcher dispatcher) {
        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(flavor, attributeSet);
        String printerNames = "";
        for (PrintService printService : printServices) {

            List<String> trays = new ArrayList<>();
            Object o = printService.getSupportedAttributeValues(Media.class, DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
            if (o instanceof Media[]) {
                for (Media media : (Media[]) o) {
                    if(media instanceof MediaTray) {
                        trays.add("'" + media + "'");
                    }
                }
            }

            printerNames += String.format("%s, trays: %s\n", printService.getName(), String.join(", ", trays));
        }
        return printerNames;
    }
}
