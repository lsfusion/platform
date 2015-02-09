package lsfusion.client;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;

public class ReportViewer extends JRViewer {
    public ReportViewer(JasperPrint print, final EditReportInvoker editInvoker) {
        super(print);

        lastFolder = SystemUtils.loadCurrentDirectory();

        ActionListener[] al = btnSave.getActionListeners();

        btnSave.removeActionListener(al[0]);

        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SystemUtils.saveCurrentDirectory(lastFolder);
            }
        });
        btnSave.addActionListener(al[0]);

        if (editInvoker != null) {
            tlbToolBar.add(Box.createHorizontalStrut(10));

            JButton editReportButton = new JButton(new ImageIcon(Main.class.getResource("/images/editReport.png")));
            editReportButton.setToolTipText(ClientResourceBundle.getString("layout.menu.file.edit.report"));
            editReportButton.setMargin(new Insets(2, 2, 2, 2));
            editReportButton.setMaximumSize(new Dimension(23, 23));
            editReportButton.setMinimumSize(new Dimension(23, 23));
            editReportButton.setPreferredSize(new Dimension(23, 23));
            editReportButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        editInvoker.invokeEditReport();
                    } catch (RemoteException e1) {
                        Throwables.propagate(e1);
                    }
                }
            });
            tlbToolBar.add(editReportButton);
        }
    }
    
    public void clickBtnPrint() {
        btnPrint.doClick();
    }

    public double getRealZoom() {
        return realZoom;
    }
}
