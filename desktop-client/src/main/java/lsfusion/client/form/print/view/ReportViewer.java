package lsfusion.client.form.print.view;

import lsfusion.base.LocalizeUtils;
import lsfusion.base.file.IOUtils;
import lsfusion.client.base.log.ClientLoggers;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerPanel;
import net.sf.jasperreports.swing.JRViewerToolbar;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.trim;

public class ReportViewer extends JRViewer {
    private ReportViewerPanel viewerPanel;

    public ReportViewer(JasperPrint print, final String printerName, boolean useDefaultPrinterInPrintIfNotSpecified, final EditReportInvoker editInvoker) {
        super(print, Locale.getDefault(), createBundle(Locale.getDefault()));
        getToolbar().modify(printerName == null && useDefaultPrinterInPrintIfNotSpecified ? getDefaultPrinterName() : printerName, editInvoker);
    }

    private static ResourceBundle createBundle(Locale locale) {
        final ResourceBundle client = LocalizeUtils.getBundle("ClientResourceBundle", locale);
        final ResourceBundle jasper = ResourceBundle.getBundle("net/sf/jasperreports/view/viewer", locale);
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                try {
                    return client.getObject("print." + key);
                } catch (MissingResourceException e) {
                    try {
                        return jasper.getObject(key);
                    } catch (MissingResourceException e2) {
                        return null;
                    }
                }
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(new HashSet<>(Collections.list(jasper.getKeys())));
            }
        };
    }

    private String getDefaultPrinterName() {
        try {
            Process p = Runtime.getRuntime().exec("wmic printer where default=true get name");
            String cmdOut = IOUtils.readStreamToString(p.getInputStream(), "cp866");
            p.waitFor();
            int exitValue = p.exitValue();
            if (exitValue == 0) {
                Pattern pattern = Pattern.compile("Name[\\s]*(.*)");
                Matcher matcher = pattern.matcher(cmdOut);
                return matcher.find() ? trim(matcher.group(1)) : null;
            } else {
                ClientLoggers.clientLogger.error("Failed to get default printer name");
                return null;
            }
        } catch (InterruptedException | IOException e) {
            ClientLoggers.clientLogger.error("Failed to get default printer name", e);
            return null;
        }
    }

    @Override
    protected JRViewerToolbar createToolbar() {
        return new ReportViewerToolbar(viewerContext, this);
    }

    public ReportViewerToolbar getToolbar() {
        return (ReportViewerToolbar) tlbToolBar;
    }

    @Override
    protected JRViewerPanel createViewerPanel() {
        viewerPanel = new ReportViewerPanel(viewerContext);
        return viewerPanel;
    }

    public void clickBtnPrint() {
        getToolbar().clickBtnPrint();
    }

    public double getRealZoom() {
        return viewerPanel.getRealZoom();
    }
}
