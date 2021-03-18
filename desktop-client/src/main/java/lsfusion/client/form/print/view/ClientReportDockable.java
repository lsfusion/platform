package lsfusion.client.form.print.view;

import bibliothek.gui.dock.common.event.CKeyboardListener;
import bibliothek.gui.dock.common.intern.CDockable;
import com.google.common.base.Throwables;
import lsfusion.client.base.view.ClientDockable;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.controller.FormsController;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.export.XlsReportConfiguration;
import net.sf.jasperreports.swing.JRViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class ClientReportDockable extends ClientDockable {
    public Integer pageCount;
    public ClientReportDockable(ReportGenerationData generationData, FormsController formsController, String formCaption, String printerName, EditReportInvoker editInvoker) throws ClassNotFoundException, IOException {
        super(null, formsController);

        try {
            final JasperPrint print = new ReportGenerator(generationData).createReport(FormPrintType.PRINT, MainController.remoteLogics);
            print.setProperty(XlsReportConfiguration.PROPERTY_DETECT_CELL_TYPE, "true");
            this.pageCount = print.getPages().size();
            final ReportViewer reportViewer = new ReportViewer(print, printerName, editInvoker);
            setContent(prepareViewer(reportViewer));
            setTitleText(formCaption);

            addKeyboardListener(new CKeyboardListener() {
                @Override
                public boolean keyPressed(CDockable cDockable, KeyEvent keyEvent) {
                    return false;
                }

                @Override
                public boolean keyReleased(CDockable cDockable, KeyEvent keyEvent) {
                    int KEY_P = 80;
                    boolean ctrlPressed = (keyEvent.getModifiers() & InputEvent.CTRL_MASK) != 0;
                    if(keyEvent.getKeyCode() == KEY_P && ctrlPressed) {
                        reportViewer.clickBtnPrint();
                    }
                    return false;
                }

                @Override
                public boolean keyTyped(CDockable cDockable, KeyEvent keyEvent) {
                    return false;
                }
            });
        } catch (JRException e) {
            Throwables.propagate(e);
        }
    }

    private JRViewer prepareViewer(final JRViewer viewer) {
        SwingUtilities.invokeLater(() -> viewer.setZoomRatio(1));
        return viewer;
    }

    @Override
    public void onOpened() {
    }

    @Override
    protected boolean focusDefaultComponent() {
        Container container = getContentPane();
        FocusTraversalPolicy traversalPolicy = container.getFocusTraversalPolicy();
        if (traversalPolicy != null) {
            Component defaultComponent = traversalPolicy.getDefaultComponent(container);
            if (defaultComponent != null) {
                return defaultComponent.requestFocusInWindow();
            }
        }
        return false;
    }
}
