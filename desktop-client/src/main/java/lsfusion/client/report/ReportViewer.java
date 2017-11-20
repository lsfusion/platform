package lsfusion.client.report;

import lsfusion.client.EditReportInvoker;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerPanel;
import net.sf.jasperreports.swing.JRViewerToolbar;

public class ReportViewer extends JRViewer {
    private ReportViewerPanel viewerPanel;

    public ReportViewer(JasperPrint print, final String printerName, final EditReportInvoker editInvoker) {
        super(print);
        getToolbar().modify(printerName, editInvoker);
    }

    @Override
    protected JRViewerToolbar createToolbar() {
        return new ReportViewerToolbar(viewerContext);
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
