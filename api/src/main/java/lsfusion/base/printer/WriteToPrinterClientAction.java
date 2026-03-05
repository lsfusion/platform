package lsfusion.base.printer;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;


public class WriteToPrinterClientAction implements ClientAction {
    public final String text;
    public final String charset;
    public final String printerName;

    public WriteToPrinterClientAction(String text, String charset, String printerName) {
        this.text = text;
        this.charset = charset;
        this.printerName = printerName;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) {

        if (text != null) {
            try {
                DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
                PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();

                PrintService[] printServices = PrintServiceLookup.lookupPrintServices(flavor, attributeSet);
                PrintService service = null;
                StringBuilder printerNames = new StringBuilder();
                for (PrintService printService : printServices) {
                    if (printService.getName().equals(printerName)) {
                        service = printService;
                        break;
                    } else {
                        printerNames.append(printService.getName()).append('\n');
                    }
                }
                if(printerNames.length() == 0)
                    printerNames = new StringBuilder("No available printers");
                else
                    printerNames.insert(0, "Available printers:\n");

                if (service != null) {

                    DocPrintJob job = service.createPrintJob();
                    Doc doc = new SimpleDoc(new ByteArrayInputStream(text.getBytes(Charset.forName(charset))), flavor, null);

                    PrintJobWatcher pjDone = new PrintJobWatcher(job);

                    job.print(doc, attributeSet);

                    pjDone.waitForDone();
                } else {
                    return String.format("Printer %s not found.\n%s", printerName, printerNames);
                }
            } catch (PrintException e) {
                throw Throwables.propagate(e);
            }
        }
        return null;
    }

    class PrintJobWatcher {
        boolean done = false;

        PrintJobWatcher(DocPrintJob job) {
            job.addPrintJobListener(new PrintJobAdapter() {
                public void printJobCanceled(PrintJobEvent pje) {
                    allDone();
                }

                public void printJobCompleted(PrintJobEvent pje) {
                    allDone();
                }

                public void printJobFailed(PrintJobEvent pje) {
                    allDone();
                }

                public void printJobNoMoreEvents(PrintJobEvent pje) {
                    allDone();
                }

                void allDone() {
                    synchronized (PrintJobWatcher.this) {
                        done = true;
                        PrintJobWatcher.this.notify();
                    }
                }
            });
        }

        public synchronized void waitForDone() {
            try {
                while (!done) {
                    wait();
                }
            } catch (InterruptedException ignored) {
            }
        }
    }
}
