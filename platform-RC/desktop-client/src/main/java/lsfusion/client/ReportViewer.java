package lsfusion.client;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;
import lsfusion.client.exceptions.ClientExceptionManager;
import lsfusion.client.form.RmiQueue;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.rmi.RemoteException;

public class ReportViewer extends JRViewer {
    public ReportViewer(JasperPrint print, final String printerName, final EditReportInvoker editInvoker) {
        super(print);

        lastFolder = SystemUtils.loadCurrentDirectory();

        final ActionListener[] al = btnSave.getActionListeners();

        btnSave.removeActionListener(al[0]);

        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SystemUtils.saveCurrentDirectory(lastFolder);
            }
        });

        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    al[0].actionPerformed(evt);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(getParent(),
                            String.format("Произошла ошибка при сохранении файла: \n%s", e.getMessage()), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ClientExceptionManager.reportClientThrowable(e);
                }
            }
        });

        if(printerName != null) {
            btnPrint.removeActionListener(btnPrint.getActionListeners()[0]);
            btnPrint.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    PrintService[] printers = PrintServiceLookup.lookupPrintServices(null, null);
                    PrintService printer = null;
                    for (PrintService p : printers) {
                        if (p.getName().equals(printerName)) {
                            printer = p;
                            break;
                        }
                    }
                    try {
                        PrinterJob pj = PrinterJob.getPrinterJob();
                        pj.setPrintService(printer != null ? printer : PrintServiceLookup.lookupDefaultPrintService());
                        pj.printDialog();
                    } catch (PrinterException e1) {
                        e1.printStackTrace();
                    }

                }
            });
        }

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
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                editInvoker.invokeEditReport();
                            } catch (RemoteException e1) {
                                Throwables.propagate(e1);
                            }
                        }
                    });
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
