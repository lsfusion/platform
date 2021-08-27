package lsfusion.client.tooltip;

import com.google.common.base.Throwables;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.object.table.tree.GroupTreeTableModel;
import lsfusion.client.form.object.table.tree.view.TreeGroupTable;
import lsfusion.client.form.property.ClientPropertyDraw;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.positioners.BasicBalloonTipPositioner;
import net.java.balloontip.styles.ToolTipBalloonStyle;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
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
                tooltipPanel = createTooltipPanel(property.getTooltipText(((GroupTreeTableModel) model).getColumnName(index)),
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

    private static JComponent createDebugLinkComponent(String path, String creationPath, String projectLSFDir, boolean isLocal, Timer closeTimer) {
        JPanel horizontalPanel = new JPanel(new HorizontalLayout());
        JTextPane showInEditorLink = getLinkComponent(projectLSFDir, creationPath, path, closeTimer);
        showInEditorLink.addHyperlinkListener(e -> {
            try {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                    //stub for using custom-protocol
                    URL url = new URL(null, e.getDescription(), new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) {
                            return null;
                        }
                    });
                    Desktop.getDesktop().browse(url.toURI());
                }
            } catch (IOException | URISyntaxException ex) {
                throw Throwables.propagate(ex);
            }
        });

        horizontalPanel.add(showInEditorLink);

        //possibility change the path
        if (!isLocal) {
            JButton userDebugPathResetButton = new JButton(new ImageIcon(ClientImages.get("view_hide.png").getImage()));
            userDebugPathResetButton.setContentAreaFilled(false);
            userDebugPathResetButton.setBorderPainted(false);
            userDebugPathResetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            userDebugPathResetButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    MainController.userDebugPath = null;
                    balloonTip.hide();
                }
            });
            addDefaultComponentMouseListeners(userDebugPathResetButton, closeTimer);

            horizontalPanel.add(userDebugPathResetButton);
        }

        return horizontalPanel;
    }

    private static JPanel createTooltipPanel(String tooltipText, String path, String creationPath, Timer closeTimer) {
        JPanel tooltipPanel = new JPanel(new VerticalLayout());
        tooltipPanel.add(new JLabel(tooltipText));
        addDefaultComponentMouseListeners(tooltipPanel, closeTimer);

        if (MainController.showDetailedInfo) {
            String projectLSFDir = MainController.projectLSFDir;

            if (projectLSFDir != null) { // when running locally
//            if (projectLSFDir == null) { // when running locally
                tooltipPanel.add(createDebugLinkComponent(path, creationPath, projectLSFDir, true, closeTimer));
            } else {
                if (MainController.userDebugPath == null) {
                    JPanel verticalPanel = new JPanel(new VerticalLayout());
                    verticalPanel.setVisible(false);

                    JTextPane fakeShowInEditorLink = getLinkComponent(null, creationPath, path, closeTimer);
                    fakeShowInEditorLink.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            fakeShowInEditorLink.setVisible(false);
                            verticalPanel.setVisible(true);
                            int height = 0;
                            Component[] components = verticalPanel.getComponents();
                            for (Component component : components) {
                                height += component.getHeight();
                            }
                            balloonTip.setSize(balloonTip.getWidth(), balloonTip.getHeight() + height - fakeShowInEditorLink.getHeight());
                        }
                    });

                    tooltipPanel.add(fakeShowInEditorLink);

                    JLabel topLabel = new JLabel(getString("debug.path.not.configured"));
                    topLabel.setForeground(Color.RED);
                    verticalPanel.add(topLabel);

                    JButton useDefaultPathButton = new JButton(getString("use.default.path"));
                    useDefaultPathButton.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            MainController.userDebugPath = "use_default_path";
                            balloonTip.hide();
                        }
                    });
                    addDefaultComponentMouseListeners(useDefaultPathButton, closeTimer);
                    verticalPanel.add(useDefaultPathButton);

                    JButton selectProjectDirButton = new JButton(getString("select.lsfusion.dir"));
                    selectProjectDirButton.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            JFileChooser fileChooser = new JFileChooser();
                            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            fileChooser.setDialogTitle(getString("select.lsfusion.dir"));
                            MainController.userDebugPath =
                                    fileChooser.showOpenDialog(verticalPanel) == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile().getAbsolutePath() : null;
                        }
                    });
                    addDefaultComponentMouseListeners(selectProjectDirButton, closeTimer);
                    verticalPanel.add(selectProjectDirButton);

                    tooltipPanel.add(verticalPanel);
                } else {
                    tooltipPanel.add(createDebugLinkComponent(path, creationPath, MainController.userDebugPath, false, closeTimer));
                }
            }
        }
        return tooltipPanel;
    }

    private static JTextPane getLinkComponent(String projectLSFDir, String creationPath, String path, Timer closeTimer) {
        JTextPane showInEditorLink = new JTextPane();
        showInEditorLink.setEditable(false);
        showInEditorLink.setContentType("text/html");

        String link;
        if (projectLSFDir != null && creationPath != null) {
            //use "**" instead "="
            String command = "--line**" + Integer.parseInt(creationPath.substring(creationPath.lastIndexOf("(") + 1, creationPath.lastIndexOf(":"))) +
                    "&path**" + Paths.get(projectLSFDir, path);
            //replace spaces and slashes because this link going through url
            link = "<a href=\"lsfusion-protocol://" + command.replaceAll(" ", "++").replaceAll("\\\\", "/") +
                    "\" target=\"_blank\">" + getString("show.in.editor") + "</a>";
        } else {
            link = "<a href=\"\">" + getString("show.in.editor") + "</a>";
        }
        showInEditorLink.setText(link);
        addDefaultComponentMouseListeners(showInEditorLink, closeTimer);

        return showInEditorLink;
    }

    private static void addDefaultComponentMouseListeners(JComponent component, Timer timer) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                timer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                timer.start();
            }
        });
    }
}
