package lsfusion.client.tooltip;

import com.google.common.base.Throwables;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.object.table.tree.GroupTreeTableModel;
import lsfusion.client.form.object.table.tree.view.TreeGroupTable;
import lsfusion.client.form.property.ClientPropertyDraw;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.positioners.BasicBalloonTipPositioner;
import net.java.balloontip.styles.ToolTipBalloonStyle;
import org.jdesktop.swingx.VerticalLayout;
import sun.awt.OSInfo;

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
                new ToolTipBalloonStyle(SwingDefaults.getPanelBackground(), SwingDefaults.getComponentBorderColor()), false));

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

            balloonTip = new BalloonTip(component, tooltipPanel, new ToolTipBalloonStyle(SwingDefaults.getPanelBackground(),
                    SwingDefaults.getComponentBorderColor()), positioner, null);

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

    private static final String userDir = MainController.userDir;
    private static final String ideaBinPath = MainController.ideaBinPath;

    private static String getCommand(String path, String creationPath) {
        String command = null;
        Path srcPath = Paths.get(userDir, "src/main/lsfusion/");
        Path customPath = !Files.exists(srcPath) ? Paths.get(userDir, path) : Paths.get(srcPath.toString(), path);

        if (Files.exists(customPath) && ideaBinPath != null) {
            String ideaRunCommand = null;
            boolean addQuotes = false;
            if (OSInfo.getOSType().equals(OSInfo.OSType.LINUX)) {
                ideaRunCommand = ideaBinPath + "/idea.sh";
            } else if (OSInfo.getOSType().equals(OSInfo.OSType.WINDOWS)) {
                ideaRunCommand = ideaBinPath + (Files.exists(Paths.get(ideaBinPath, "idea64.exe")) ? "/idea64.exe" : "/idea.exe");
                addQuotes = true;
            } else if (OSInfo.getOSType().equals(OSInfo.OSType.MACOSX)) {
                ideaRunCommand = "/idea";
            }

            int line = Integer.parseInt(creationPath.substring(creationPath.lastIndexOf("(") + 1, creationPath.lastIndexOf(":")));

            command = ideaRunCommand != null ? addQuotes ? "\"" + ideaRunCommand + "\"" + " --line " + line + " " + "\"" + customPath + "\"" :
                    ideaRunCommand + " --line " + line + " " + customPath : null;

        }
        return command;
    }

    private static JPanel createTooltipPanel(String tooltipText, String path, String creationPath, Timer closeTimer) {
        JPanel jPanel = new JPanel(new VerticalLayout());
        jPanel.add(new JLabel(tooltipText));
        String command = getCommand(path, creationPath);
        if (MainController.inDevMode && command != null) {
            JLabel label = new JLabel("<html><font color='#000099'><u>" + getString("show.in.editor") + "</u></font></html>");
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
                    try {
                        Runtime.getRuntime().exec(command);
                        if (balloonTip != null && balloonTip.isShowing())
                            balloonTip.closeBalloon();
                    } catch (IOException exception) {
                        Throwables.propagate(exception);
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
