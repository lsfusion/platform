package lsfusion.client.report;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;
import lsfusion.client.EditReportInvoker;
import lsfusion.client.Main;
import lsfusion.client.exceptions.ClientExceptionManager;
import lsfusion.client.form.RmiQueue;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.print.JRPrinterAWT;
import net.sf.jasperreports.swing.JRViewerController;
import net.sf.jasperreports.swing.JRViewerToolbar;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.*;
import java.rmi.RemoteException;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.SwingUtils.showConfirmDialog;

public class ReportViewerToolbar extends JRViewerToolbar {
    private final ReportViewer reportViewer;
    private JButton editReportButton = null;
    private JButton addReportButton = null;
    private JButton deleteReportButton = null;
    private Boolean hasCustomReports;
    
    public ReportViewerToolbar(JRViewerController viewerContext, ReportViewer reportViewer) {
        super(viewerContext);
        this.reportViewer = reportViewer;

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
    }
    
    protected void modify(final String printerName, EditReportInvoker editInvoker) {
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
                    printPages(printer);
                }
            });
        }

        if (editInvoker != null) {
            try {
                hasCustomReports = editInvoker.hasCustomReports();
            } catch (RemoteException e) {
                throw Throwables.propagate(e);
            }

            add(Box.createHorizontalStrut(10));

            editReportButton = getEditReportButton(editInvoker, hasCustomReports);
            deleteReportButton = getDeleteReportButton(editInvoker, hasCustomReports);
            addReportButton = getAddReportButton(editInvoker);

            add(editReportButton);
            add(addReportButton);
            add(deleteReportButton);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void printPages(PrintService printer) {
        try {
            JasperPrint jasperPrint = viewerContext.getJasperPrint();
            JasperReportsContext jasperReportsContext = viewerContext.getJasperReportsContext();
            if (printer == null) { // хоть в else и почти полный копипаст, всё равно вызовем стандартный метод при отсутствии принтера
                JasperPrintManager.getInstance(jasperReportsContext).print(jasperPrint, true);
            } else {
                PrinterJob pj = PrinterJob.getPrinterJob();
                pj.setPrintService(printer);

                // c/p from JRPrinterAWT.printPages()
                PageFormat pageFormat = pj.defaultPage();
                Paper paper = pageFormat.getPaper();
                int pageWidth = jasperPrint.getPageWidth();
                int pageHeight = jasperPrint.getPageHeight();
                switch (jasperPrint.getOrientationValue()) {
                    case LANDSCAPE:
                        pageFormat.setOrientation(PageFormat.LANDSCAPE);
                        paper.setSize(pageHeight, pageWidth);
                        paper.setImageableArea(0, 0, pageHeight, pageWidth);
                        break;
                    case PORTRAIT:
                    default:
                        pageFormat.setOrientation(PageFormat.PORTRAIT);
                        paper.setSize(pageWidth, pageHeight);
                        paper.setImageableArea(0, 0, pageWidth, pageHeight);
                }
                pageFormat.setPaper(paper);

                Book book = new Book();
                
                book.append(new JRPrinterAWT(jasperReportsContext, jasperPrint), pageFormat, jasperPrint.getPages().size());
                pj.setPageable(book);

                if (pj.printDialog()) {
                    pj.print();
                }
            }
        } catch (PrinterException | JRException e1) {
            Throwables.propagate(e1);
        }
    }

    private JButton getEditReportButton(final EditReportInvoker editInvoker, boolean visible) {
        JButton editReportButton = new JButton(new ImageIcon(Main.class.getResource("/images/editReport.png")));
        editReportButton.setToolTipText(getString("layout.menu.file.edit.report"));
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
                        editButtonPressed(editInvoker);
                    }
                });
            }
        });
        editReportButton.setVisible(visible);
        return editReportButton;
    }

    private void editButtonPressed(EditReportInvoker editInvoker) {
        editReport(editInvoker);
    }

    private JButton getAddReportButton(final EditReportInvoker editInvoker) {
        final JButton addReportButton = new JButton(new ImageIcon(Main.class.getResource("/images/editAutoReport.png")));
        addReportButton.setToolTipText(getString("layout.menu.file.create.custom.report"));
        addReportButton.setMargin(new Insets(2, 2, 2, 2));
        addReportButton.setMaximumSize(new Dimension(23, 23));
        addReportButton.setMinimumSize(new Dimension(23, 23));
        addReportButton.setPreferredSize(new Dimension(23, 23));
        addReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        addButtonPressed(editInvoker);
                    }
                });
            }
        });
        return addReportButton;
    }

    private void addButtonPressed(EditReportInvoker editInvoker) {
        if(hasCustomReports) { // при добавлении, если есть уже сохраненные отчеты предлагаем их удалить
            if(showConfirmDialog(reportViewer, getString("layout.menu.file.create.custom.report.confirm"),
                    getString("layout.menu.file.create.custom.report.title"), JOptionPane.WARNING_MESSAGE, false) == 0) {
                recreateReport(editInvoker);
                return;
            }
        }
        
        if (!hasCustomReports) {
            addReport(editInvoker);
            editReportButton.setVisible(true);
            deleteReportButton.setVisible(true);
            invalidate();
        }
    }

    private JButton getDeleteReportButton(final EditReportInvoker editInvoker, final boolean visible) {
        final JButton deleteReportButton = new JButton(new ImageIcon(Main.class.getResource("/images/deleteReport.png")));
        deleteReportButton.setToolTipText(getString("layout.menu.file.delete.report"));
        deleteReportButton.setMargin(new Insets(2, 2, 2, 2));
        deleteReportButton.setMaximumSize(new Dimension(23, 23));
        deleteReportButton.setMinimumSize(new Dimension(23, 23));
        deleteReportButton.setPreferredSize(new Dimension(23, 23));
        deleteReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        deleteButtonPressed(editInvoker);
                    }
                });
            }
        });
        deleteReportButton.setVisible(visible);
        return deleteReportButton;
    }

    private void deleteButtonPressed(EditReportInvoker editInvoker) {
        if (showConfirmDialog(reportViewer, getString("layout.menu.file.delete.report.confirm"),
                getString("layout.menu.file.delete.report"), JOptionPane.WARNING_MESSAGE, false) == 0) {
            deleteReport(editInvoker);
            editReportButton.setVisible(false);
            deleteReportButton.setVisible(false);
            invalidate();
        }
    }

    private void addReport(EditReportInvoker editInvoker) {
        try {
            editInvoker.invokeAddReport();
        } catch (RemoteException e) {
            throw Throwables.propagate(e);
        }
        hasCustomReports = true;
    }

    private void recreateReport(EditReportInvoker editInvoker) { // нужен чтобы сохранить путь
        try {
            editInvoker.invokeRecreateReport();;
        } catch (RemoteException e) {
            throw Throwables.propagate(e);
        }
        hasCustomReports = true;
    }

    private void editReport(EditReportInvoker editInvoker) {
        try {
            editInvoker.invokeEditReport();
        } catch (RemoteException e) {
            throw Throwables.propagate(e);
        }
    }

    private void deleteReport(EditReportInvoker editInvoker) {
        try {
            editInvoker.invokeDeleteReport();
        } catch (RemoteException e) {
            throw Throwables.propagate(e);
        }
        hasCustomReports = false;
    }

    public void clickBtnPrint() {
        btnPrint.doClick();
    }
}
