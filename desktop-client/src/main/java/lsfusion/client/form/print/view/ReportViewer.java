package lsfusion.client.form.print.view;

import lsfusion.base.file.IOUtils;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerPanel;
import net.sf.jasperreports.swing.JRViewerToolbar;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.trim;

public class ReportViewer extends JRViewer {
    private ReportViewerPanel viewerPanel;

    public ReportViewer(JasperPrint print, final String printerName, boolean useDefaultPrinterInPrintIfNotSpecified, final EditReportInvoker editInvoker) {
        super(print);
        getToolbar().modify(printerName == null && useDefaultPrinterInPrintIfNotSpecified ? getDefaultPrinterName() : printerName, editInvoker);
    }

    private String getDefaultPrinterName() {
        try {
            Process p = Runtime.getRuntime().exec("wmic printer where default=true get name");
            String cmdOut = IOUtils.readStreamToString(p.getInputStream());
            p.waitFor();
            int exitValue = p.exitValue();
            if (exitValue == 0) {
                Pattern pattern = Pattern.compile("Name[\\s]*(.*)");
                Matcher matcher = pattern.matcher(cmdOut);
                return matcher.find() ? trim(matcher.group(1)) : null;
            } else {
                throw new RuntimeException("Failed to get default printer name");
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
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
