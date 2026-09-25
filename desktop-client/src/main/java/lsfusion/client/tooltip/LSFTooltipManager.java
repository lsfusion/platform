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

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import static lsfusion.base.BaseUtils.isEmpty;
import static lsfusion.client.ClientResourceBundle.getString;

public class LSFTooltipManager {

    private static BalloonTip balloonTip;
    private static int index;
    private static final Timer closeTimer = new Timer(500, e -> closeBalloon()) {
        @Override
        public boolean isRepeats() {
            return false;
        }
    };

    public static void initTooltip(JComponent component, String tooltipText, String path, String creationPath) {
        int tooltipDelay = MainController.showDetailedInfoDelay;
        if(!isEmpty(tooltipText) && tooltipDelay > 0) {
            setComponentMouseListeners(component, new Timer(1500, evt -> balloonTip = new BalloonTip(component, createTooltipPanel(tooltipText, path, creationPath),
                    new LSFTooltipStyle(), false)));
        }
    }

    public static void initTooltip(JComponent component, Object model, JTable gridTable) {
        int tooltipDelay = MainController.showDetailedInfoDelay;
        if (tooltipDelay > 0) {
            Timer tooltipTimer = new Timer(tooltipDelay, evt -> {
                int currentIndex = index;
                JPanel tooltipPanel;
                if (gridTable instanceof GridTable) {
                    TableColumnModel columnModel = gridTable.getColumnModel();
                    if (currentIndex < 0 || currentIndex >= columnModel.getColumnCount())
                        return;

                    int modelIndex = columnModel.getColumn(currentIndex).getModelIndex();
                    GridTable table = (GridTable) gridTable;
                    tooltipPanel = createTooltipPanel(table.getModel().getColumnProperty(modelIndex).getTooltipText(table.getColumnCaption(index)),
                            table.getModel().getColumnProperty(modelIndex).path, table.getModel().getColumnProperty(modelIndex).creationPath);
                } else {
                    GroupTreeTableModel treeTableModel = (GroupTreeTableModel) model;
                    if (currentIndex < 0 || currentIndex >= treeTableModel.getColumnCount())
                        return;

                    ClientPropertyDraw property = treeTableModel.getColumnProperty(currentIndex);

                    /*if first column*/
                    if (property == null)
                        return;

                    tooltipPanel = createTooltipPanel(property.getTooltipText(treeTableModel.getColumnName(currentIndex)),
                            property.path, property.creationPath);
                }

                BasicBalloonTipPositioner positioner = new BasicBalloonTipPositioner(15, 15) {

                    @Override
                    protected void determineLocation(Rectangle attached) {
                        GraphicsEnvironment localGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

                        int balloonWidth = balloonTip.getPreferredSize().width;
                        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                        if (pointerInfo != null) {
                            Point mouseLocation = pointerInfo.getLocation();
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
                    }
                };

                closeBalloon(); //helps close tooltips on dialog opening
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

                    if (!tooltipTimer.isRunning() && balloonTip != null && !balloonTip.isShowing() && newIndex != -1)
                        tooltipTimer.start();
                    else if (index != newIndex)
                        closeBalloon();

                    index = newIndex;
                }
            });

            setComponentMouseListeners(component, tooltipTimer);
        }
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

    private static void setComponentMouseListeners(JComponent component, Timer tooltipTimer) {
        tooltipTimer.setRepeats(false);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBalloon();
                tooltipTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tooltipTimer.stop();
                if (balloonTip != null && balloonTip.isShowing() && !closeTimer.isRunning())
                    closeTimer.start();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                closeBalloon();
            }
        });
    }

    private static JPanel createTooltipPanel(String tooltipText, String path, String creationPath) {
        JPanel tooltipPanel = new JPanel(new VerticalLayout());
        tooltipPanel.add(new JLabel(tooltipText));
        addDefaultComponentMouseListeners(tooltipPanel);

        if (MainController.showDetailedInfo && creationPath != null && path != null) {
            tooltipPanel.add(getLinkComponent(creationPath, path));
        }
        return tooltipPanel;
    }

    private static final String OPEN_IN_IDE = "open-in-ide";

    private static JTextPane getLinkComponent(String creationPath, String path) {
        JTextPane showInEditorLink = new JTextPane();
        showInEditorLink.setEditable(false);
        showInEditorLink.setContentType("text/html");
        String link = "<a href=\"" + OPEN_IN_IDE + "\">" + getString("show.in.editor") + "</a> &ensp; " + "(<a href=\"https://github.com/lsfusion/platform/issues/649\" target=\"_blank\"> ? </a>)";
        showInEditorLink.setText(link);
        addDefaultComponentMouseListeners(showInEditorLink);

        showInEditorLink.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                if (OPEN_IN_IDE.equals(e.getDescription())) {
                    openInIDE(creationPath, path);
                } else {
                    try {
                        Desktop.getDesktop().browse(new URI(e.getDescription()));
                    } catch (IOException | URISyntaxException ex) {
                        throw Throwables.propagate(ex);
                    }
                }
                closeBalloon();
            }
        });

        return showInEditorLink;
    }

    // creationPath is "Module(line:column)" (1-based, the column may carry a trailing meta marker), path is the module
    // file relative to the source root. The lsFusion IDEA plugin serves /api/lsfusion-open on the IDE's built-in web
    // server, which takes the first free port from 63342 up; the ports are tried in turn until an IDE answers 200
    // (a 404 is another IDE without the plugin). In turn, not at once: two IDEs with the plugin would both open the
    // file. The long read timeout is for the IDE itself, which asks the user whether to trust this client before
    // answering the first request; a stalled unrelated service on one of these JetBrains ports would cost a minute.
    private static void openInIDE(String creationPath, String path) {
        String position = creationPath.substring(creationPath.lastIndexOf("(") + 1, creationPath.lastIndexOf(")"));
        int line = Integer.parseInt(position.substring(0, position.indexOf(":")));
        int column = Integer.parseInt(position.substring(position.indexOf(":") + 1).replaceAll("[^0-9]", ""));
        String query;
        try {
            query = "path=" + URLEncoder.encode(path, "UTF-8") + "&line=" + line + "&column=" + column;
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        Thread probe = new Thread(() -> {
            for (int port = 63342; port <= 63352; port++) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/api/lsfusion-open?" + query).openConnection();
                    connection.setConnectTimeout(500);
                    connection.setReadTimeout(60000); // the IDE may be asking the user whether to trust this client
                    int status = connection.getResponseCode();
                    connection.disconnect();
                    if (status == HttpURLConnection.HTTP_OK)
                        return;
                } catch (IOException ignored) { // nothing listens on this port
                }
            }
        }, "lsfusion-open-in-ide");
        probe.setDaemon(true); // a stalled port must not keep the client alive on exit
        probe.start();
    }

    private static void addDefaultComponentMouseListeners(JComponent component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeTimer.start();
            }
        });
    }

    //We need to manually manage tooltip closure because we have support for tooltips on table and tree headers
    private static void closeBalloon() {
        if (balloonTip != null && balloonTip.isShowing()) {
            Container parent = balloonTip.getParent();
            for (Component parentComponent : parent.getComponents()) {
                if (parentComponent instanceof BalloonTip)
                    parent.remove(parentComponent);
            }
            parent.revalidate();
            parent.repaint();
        }
    }
}
