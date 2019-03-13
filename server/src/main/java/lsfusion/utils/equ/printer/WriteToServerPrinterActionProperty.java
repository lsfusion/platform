package lsfusion.utils.equ.printer;

import com.google.common.base.Throwables;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Iterator;

public class WriteToServerPrinterActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface printerNameInterface;


    public WriteToServerPrinterActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
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

            String result = print(text, charset, printerName);
            findProperty("printed[]").change(result == null ? true : null, context);
            if (result != null)
                ServerLoggers.printerLogger.error(result);
            else
                ServerLoggers.printerLogger.info("Printer Job for printer " + printerName + " finished successfully");
        } catch (Exception e) {
            ServerLoggers.printerLogger.error("WriteToPrinter error", e);
            try {
                findProperty("printed[]").change((Boolean) null, context);
            } catch (ScriptingErrorLog.SemanticErrorException ignored) {
            }
            throw Throwables.propagate(e);
        }

    }

    public String print(String text, String charset, String printerName) throws IOException {

        if (text != null) {
            try {
                DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
                PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();

                PrintService[] printServices = PrintServiceLookup.lookupPrintServices(flavor, attributeSet);
                PrintService service = null;
                String printerNames = "";
                for (PrintService printService : printServices) {
                    if (printService.getName().equals(printerName)) {
                        service = printService;
                        break;
                    } else {
                        printerNames += printService.getName() + '\n';
                    }
                }
                if(printerNames.isEmpty())
                    printerNames = "Нет доступных принтеров";
                else
                    printerNames = "Доступны принтеры:\n" + printerNames;

                if (service != null) {

                    DocPrintJob job = service.createPrintJob();
                    Doc doc = new SimpleDoc(new ByteArrayInputStream(text.getBytes(Charset.forName(charset))), flavor, null);

                    PrintJobWatcher pjDone = new PrintJobWatcher(job);

                    job.print(doc, attributeSet);

                    pjDone.waitForDone();
                } else {
                    return String.format("Принтер %s не найден.\n%s", printerName, printerNames);
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