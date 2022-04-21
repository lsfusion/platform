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
    private static final Timer closeTimer = new Timer(500, e -> closeBalloon()) {
        @Override
        public boolean isRepeats() {
            return false;
        }
    };

    public static void initTooltip(JComponent component, String tooltipText, String path, String creationPath) {
        setComponentMouseListeners(component, new Timer(1500, evt -> balloonTip = new BalloonTip(component, createTooltipPanel(tooltipText, path, creationPath),
                new LSFTooltipStyle(), false)));
    }

    public static void initTooltip(JComponent component, Object model, JTable gridTable) {
        Timer tooltipTimer = new Timer(1500, evt -> {

            JPanel tooltipPanel;
            if (gridTable instanceof GridTable) {
                int modelIndex = gridTable.getColumnModel().getColumn(index).getModelIndex();
                GridTable table = (GridTable) gridTable;
                tooltipPanel = createTooltipPanel(table.getModel().getColumnProperty(modelIndex).getTooltipText(table.getColumnCaption(index)),
                        table.getModel().getColumnProperty(modelIndex).path, table.getModel().getColumnProperty(modelIndex).creationPath);
            } else {
                ClientPropertyDraw property = ((GroupTreeTableModel) model).getColumnProperty(index);

                //if first column
                if (property == null)
                    return;

                tooltipPanel = createTooltipPanel(property.getTooltipText(((GroupTreeTableModel) model).getColumnName(index)),
                        property.path, property.creationPath);
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

                if (!tooltipTimer.isRunning() && balloonTip != null && !balloonTip.isShowing() && newIndex != -1)
                    tooltipTimer.start();
                else if (index != newIndex)
                    closeBalloon();

                index = newIndex;
            }
        });

        setComponentMouseListeners(component, tooltipTimer);
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
            String projectLSFDir = MainController.projectLSFDir;

            if (projectLSFDir != null) {
                tooltipPanel.add(getLinkComponent(projectLSFDir, creationPath, path));
            } else {
                JPanel buttonPanel = new JPanel(new VerticalLayout());
                buttonPanel.setVisible(false);

                JButton selectProjectDirButton = new JButton(MainController.userDebugPath == null ? getString("select.lsfusion.dir") : "reset");
                selectProjectDirButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (MainController.userDebugPath == null) {
                            JFileChooser fileChooser = new JFileChooser();
                            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            fileChooser.setDialogTitle(getString("select.lsfusion.dir"));
                            MainController.userDebugPath =
                                    fileChooser.showOpenDialog(tooltipPanel) == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile().getAbsolutePath() : null;

                        } else {
                            MainController.userDebugPath = null;
                        }
                        closeBalloon();
                    }
                });
                addDefaultComponentMouseListeners(selectProjectDirButton);
                buttonPanel.add(selectProjectDirButton);

                JPanel horizontalPanel = new JPanel(new HorizontalLayout());
                horizontalPanel.add(getLinkComponent(MainController.userDebugPath == null ? "use_default_path" : MainController.userDebugPath, creationPath, path));

                JButton preferencesButton = new JButton(new ImageIcon(ClientImages.get("userPreferences.png").getImage()));
                preferencesButton.setContentAreaFilled(false);
                preferencesButton.setBorderPainted(false);
                preferencesButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

                preferencesButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        boolean panelVisible = buttonPanel.isVisible();
                        buttonPanel.setVisible(!panelVisible);

                        int height = buttonPanel.getComponent(0).getHeight();

                        balloonTip.setSize(balloonTip.getWidth(), balloonTip.getHeight() + (panelVisible ? -height : height));
                    }
                });
                addDefaultComponentMouseListeners(preferencesButton);
                horizontalPanel.add(preferencesButton);

                tooltipPanel.add(horizontalPanel);
                tooltipPanel.add(buttonPanel);
            }
        }
        return tooltipPanel;
    }

    private static JTextPane getLinkComponent(String projectLSFDir, String creationPath, String path) {
        JTextPane showInEditorLink = new JTextPane();
        showInEditorLink.setEditable(false);
        showInEditorLink.setContentType("text/html");
        //use "**" instead "="
        String command = "--line**" + Integer.parseInt(creationPath.substring(creationPath.lastIndexOf("(") + 1, creationPath.lastIndexOf(":"))) +
                "&path**" + Paths.get(projectLSFDir, path);
        //replace spaces and slashes because this link going through url
        String link = "<a href=\"lsfusion-protocol://" + command.replaceAll(" ", "++").replaceAll("\\\\", "/") +
                "\" target=\"_blank\">" + getString("show.in.editor") + "</a> &ensp; " + "(<a href=\"https://github.com/lsfusion/platform/issues/649\" target=\"_blank\"> ? </a>)";
        showInEditorLink.setText(link);
        addDefaultComponentMouseListeners(showInEditorLink);

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
                    closeBalloon();
                }
            } catch (IOException | URISyntaxException ex) {
                throw Throwables.propagate(ex);
            }
        });

        return showInEditorLink;
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
