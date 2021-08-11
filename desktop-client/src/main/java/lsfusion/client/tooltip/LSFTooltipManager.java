package lsfusion.client.tooltip;

import com.google.common.base.Throwables;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.object.table.tree.GroupTreeTableModel;
import lsfusion.client.form.object.table.tree.view.TreeGroupTable;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.view.MainFrame;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.positioners.BasicBalloonTipPositioner;
import net.java.balloontip.styles.ToolTipBalloonStyle;
import org.apache.commons.lang3.SystemUtils;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static lsfusion.client.ClientResourceBundle.getString;

public class LSFTooltipManager {

    private static BalloonTip balloonTip;
    private static int index;

    public static void initTooltip(JComponent component, String tooltipText, String path, String creationPath) {
        Timer closeTimer = getCloseTimer();

        Timer tooltipTimer = new Timer(1500, evt -> balloonTip = new BalloonTip(component, createTooltipPanel(tooltipText, path, creationPath, closeTimer),
                new LSFTooltipStyle(), false));

        setComponentMouseListeners(component, closeTimer, tooltipTimer);
    }

    public static void initTooltip(JComponent component, Object model, JTable gridTable) {
        Timer closeTimer = getCloseTimer();

        Timer tooltipTimer = new Timer(1500, evt -> {

            JPanel tooltipPanel;
            if (gridTable instanceof GridTable) {
                int modelIndex = gridTable.getColumnModel().getColumn(index).getModelIndex();
                GridTable table = (GridTable) gridTable;
                tooltipPanel = createTooltipPanel(table.getModel().getColumnProperty(modelIndex).getTooltipText(table.getColumnCaption(index)),
                        table.getModel().getColumnProperty(modelIndex).path, table.getModel().getColumnProperty(modelIndex).creationPath, closeTimer);
            } else {
                ClientPropertyDraw property = ((TreeGroupTable) gridTable).getProperty(0, index);
                tooltipPanel = createTooltipPanel(property.getTooltipText(((GroupTreeTableModel)model).getColumnName(index)),
                        property.path, property.creationPath, closeTimer);
            }

            BasicBalloonTipPositioner positioner = new BasicBalloonTipPositioner(15, 15) {

                @Override
                protected void determineLocation(Rectangle attached) {
                    GraphicsEnvironment localGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

                    int balloonWidth = balloonTip.getPreferredSize().width;
                    Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
                    int calculatedX = mouseLocation.x - (balloonWidth / 2);
                    Rectangle bounds = localGraphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration().getBounds();

                    if (x + balloonWidth > balloonTip.getTopLevelContainer().getWidth())
                        x = balloonTip.getTopLevelContainer().getWidth() - balloonWidth;
                    else if (mouseLocation.x < bounds.width && calculatedX > 0)
                        x = calculatedX;
                    else if (mouseLocation.x - (balloonWidth / 2) > bounds.width)
                        x = calculatedX - bounds.width;
                    else
                        x = 0;

                    int calculatedY = attached.y - balloonTip.getPreferredSize().height;
                    y = calculatedY < 0 ? attached.y + attached.height : calculatedY;

                }
            };

            balloonTip = new BalloonTip(component, tooltipPanel, new LSFTooltipStyle(), positioner, null);

            //Tooltips in tables are not displayed on elements located close to the borders
            if (!balloonTip.isVisible())
                balloonTip.show();
        });

        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                tooltipTimer.stop();

                int newIndex = 0;
                if (model instanceof TableColumnModel)
                    newIndex = ((TableColumnModel) model).getColumnIndexAtX(e.getPoint().x);
                else if (model instanceof GroupTreeTableModel)
                    newIndex = ((GroupTreeTableModel) model).getColumnIndexAtX(e.getPoint().x, (TreeGroupTable) gridTable);

                if (!tooltipTimer.isRunning() && balloonTip != null && !balloonTip.isShowing() && newIndex != -1) {
                    tooltipTimer.start();
                } else if (index != newIndex && balloonTip != null && balloonTip.isShowing()) {
                    balloonTip.closeBalloon();
                }
                index = newIndex;
            }
        });

        setComponentMouseListeners(component, closeTimer, tooltipTimer);
    }

    private static class LSFTooltipStyle extends ToolTipBalloonStyle {

        private static final Color fillColor = SwingDefaults.getPanelBackground();
        private static final Color borderColor = SwingDefaults.getComponentBorderColor();

        public LSFTooltipStyle() {
            super(fillColor, borderColor);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g;
            width -= 1;
            height -= 1;

            int yTop;        // Y-coordinate of the top side of the balloon
            int yBottom;    // Y-coordinate of the bottom side of the balloon
            if (flipY) {
                yTop = y + verticalOffset;
                yBottom = y + height;
            } else {
                yTop = y;
                yBottom = y + height - verticalOffset;
            }

            // Draw the outline of the balloon
            g2d.setPaint(fillColor);
            g2d.fillRect(x, yTop, width, yBottom);
            g2d.setPaint(borderColor);
            g2d.drawRect(x, yTop, width, yBottom);

            //the lower border is not drawn by BalloonTip. Draw it manually
            g2d.drawLine(x, yBottom, x + width, yBottom);
        }
    }

    private static void setComponentMouseListeners(JComponent component, Timer closeTimer, Timer tooltipTimer) {
        tooltipTimer.setRepeats(false);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (balloonTip != null && balloonTip.isShowing())
                    balloonTip.closeBalloon();
                tooltipTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tooltipTimer.stop();
                if (balloonTip != null && balloonTip.isShowing())
                    closeTimer.start();
            }
        });
    }

    private static Timer getCloseTimer() {
        Timer timer = new Timer(500, e -> {
            if (balloonTip != null && balloonTip.isShowing())
                balloonTip.closeBalloon();
        });
        timer.setRepeats(false);
        return timer;
    }

    private static String getCommand(String path, String creationPath) {
        String ideaExecPath = MainController.ideaExecPath;
        String command = null;
        if (ideaExecPath != null) {
            Path fileAbsolutePath = Paths.get(MainFrame.debugPath, path);
            //if chosen path to the project is incorrect, the path will become null
            MainFrame.debugPath = Files.exists(fileAbsolutePath) ? MainFrame.debugPath : null;

            int line = Integer.parseInt(creationPath.substring(creationPath.lastIndexOf("(") + 1, creationPath.lastIndexOf(":")));

            command = (SystemUtils.IS_OS_WINDOWS ? "\"" + ideaExecPath + "\"" : ideaExecPath) + " --line " + line + " " + "\"" + fileAbsolutePath + "\"";
        }
        return command;
    }

    private static JPanel createTooltipPanel(String tooltipText, String path, String creationPath, Timer closeTimer) {
        JPanel jPanel = new JPanel(new VerticalLayout(10));
        jPanel.add(new JLabel(tooltipText));
        String command = path != null ? getCommand(path, creationPath) : null;
        if (MainController.inDevMode && command != null) {
            JComponent label;
            if (MainFrame.debugPath == null) {
                label = new JButton("Set path to lsfusion dir");
            } else {
                label = new JPanel(new HorizontalLayout(10));
                label.add(new JLabel("<html><font color='#000099'><u>" + getString("show.in.editor") + "</u></font></html>"));
                JButton resetButton = new JButton(new ImageIcon(ClientImages.get("view_hide.png").getImage()));
                resetButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        MainFrame.debugPath = null;
                    }
                });
                label.add(resetButton);
            }

            label.setCursor(new Cursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    closeTimer.stop();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    closeTimer.start();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (MainFrame.debugPath == null) {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        fileChooser.setDialogTitle("Select project lsfusion directory");
                        MainFrame.debugPath =
                                fileChooser.showOpenDialog(jPanel) == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile().getAbsolutePath() : null;
                    } else {
                        try {
                            Runtime.getRuntime().exec(command);
                            if (balloonTip != null && balloonTip.isShowing())
                                balloonTip.closeBalloon();
                        } catch (IOException exception) {
                            Throwables.propagate(exception);
                        }
                    }
                }
            });
            jPanel.add(label);
        }
        jPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeTimer.start();
            }
        });
        return jPanel;
    }
}
