package lsfusion.erp.utils.printer;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Iterator;

public class WriteToPrinterActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface printerNameInterface;


    public WriteToPrinterActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        textInterface = i.next();
        charsetInterface = i.next();
        printerNameInterface = i.next();

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            String text = (String) context.getDataKeyValue(textInterface).object;
            String charset = (String) context.getDataKeyValue(charsetInterface).object;
            String printerName = (String) context.getDataKeyValue(printerNameInterface).object;

            if (text != null) {
                DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
                PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();

                PrintService[] printServices = PrintServiceLookup.lookupPrintServices(flavor, attributeSet);
                PrintService service = null;
                for(PrintService printService : printServices) {
                    if(printService.getName().equals(printerName)) {
                        service = printService;
                        break;
                    }
                }

                if (service != null) {

                    DocPrintJob job = service.createPrintJob();
                    Doc doc = new SimpleDoc(new ByteArrayInputStream(text.getBytes(Charset.forName(charset))), flavor, null);

                    PrintJobWatcher pjDone = new PrintJobWatcher(job);

                    job.print(doc, attributeSet);

                    pjDone.waitForDone();
                } else {
                    context.requestUserInteraction(new MessageClientAction(String.format("Принтер %s не найден", printerName), "Ошибка"));
                }
            }
        } catch (PrintException e) {
            throw Throwables.propagate(e);
        }

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
