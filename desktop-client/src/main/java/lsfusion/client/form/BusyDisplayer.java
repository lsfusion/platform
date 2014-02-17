package lsfusion.client.form;

import lsfusion.base.Provider;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Timer;
import java.util.TimerTask;

public class BusyDisplayer extends TimerTask {
    public static int INITIAL_WAIT_PERIOD = 1000;
    public static int REPAINT_PERIOD = 200;
    public static int UPDATE_PERIOD = 1000;

    private Timer executionTimer;
    private Window drawingWindow;
    private final Provider<String> serverMessageProvider;

    private long lastUpdateTime = -UPDATE_PERIOD;
    private String currentMessage = null;
    private int segmentCount = 7;
    private int count = segmentCount;


    public BusyDisplayer(Provider<String> serverMessageProvider) {
        this.serverMessageProvider = serverMessageProvider;
    }

    public void start() {
        drawingWindow = SwingUtils.getActiveVisibleWindow();
        if (drawingWindow == null) {
            drawingWindow = Main.frame;
        }

        if (drawingWindow != null) {
            drawingWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            executionTimer = new Timer();
            executionTimer.schedule(this, INITIAL_WAIT_PERIOD, REPAINT_PERIOD);
        }
    }

    public void stop() {
        if (drawingWindow != null) {
            executionTimer.cancel();
            drawingWindow.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            drawingWindow.repaint();
        }
    }

    @Override
    public void run() {
        if (drawingWindow != null) {
            initializeIfNeeded(drawingWindow);
            drawProgressBar(drawingWindow);
        }
    }

    private boolean initialized = false;
    public void initializeIfNeeded(Window window) {
        if (!initialized) {
            Graphics gr = window.getGraphics();
            if (gr != null) {
//                Image image = blur(blur(getScreen(window)));
                Image image = darken(getScreen(window));
                gr.drawImage(image, 0, 0, null);
            }
            initialized = true;
        }
    }

    private void drawProgressBar(Window window) {
        Component canvas = null;
        if (window instanceof JDialog) {
            canvas = ((JDialog) window).getRootPane();
        } else if (window instanceof JFrame) {
            canvas = ((JFrame) window).getRootPane();
        }

        Graphics gr = window.getGraphics();
        if (gr != null && canvas != null) {
            Font loadingTextFont = new Font("Dialog", Font.BOLD, 15);
            String loadingText = ClientResourceBundle.getString("form.loading");
            int segmentHeight = 25;
            int segmentWidth = 10;
            int segmentGap = 2;
            int maxSegmentCount = (int) (canvas.getWidth() / (segmentWidth + segmentGap) * 0.3);
            if (segmentCount > maxSegmentCount) {
                segmentCount = maxSegmentCount;
                count = segmentCount;
            }

            int loadingTextWidth = gr.getFontMetrics(loadingTextFont).stringWidth(loadingText);
            int loadingTextHeight = gr.getFontMetrics(loadingTextFont).getHeight();

            if (Main.configurationAccessAllowed) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime >= UPDATE_PERIOD) {
                    try {
                        currentMessage = serverMessageProvider.get();
                    } catch (Throwable ignore) {
                    }
                    lastUpdateTime = currentTime;
                }
            }

            int barWidth = maxSegmentCount * segmentWidth + (maxSegmentCount - 1) * (segmentGap);
            int rectHeight = segmentHeight + loadingTextHeight + 23;
            int rectWidth = Math.max(barWidth, loadingTextWidth) + 20;
            int rectX = canvas.getX() + (canvas.getWidth() - Math.max(barWidth, loadingTextWidth)) / 2 - 10;
            int rectY = canvas.getY() + (canvas.getHeight() - rectHeight) / 2;
            int messageRectWidth = canvas.getWidth() - 40;
            int messageRectHeight = 100;
            int messageRectX = 20;
            int messageRectY = canvas.getHeight() - 100;
            int barX = rectX + 10;
            int barY = rectY + loadingTextHeight + 10;

            if (loadingTextWidth > barWidth) {
                maxSegmentCount = loadingTextWidth / (segmentWidth + segmentGap);
            }

            gr.setColor(Color.DARK_GRAY);
            gr.fillRoundRect(rectX, rectY, rectWidth + 2, rectHeight + 2, 5, 5);
            gr.setColor(Color.WHITE);
            gr.fillRoundRect(rectX, rectY, rectWidth, rectHeight, 5, 5);
            if (Main.configurationAccessAllowed) {
                gr.fillRoundRect(messageRectX, messageRectY, messageRectWidth, messageRectHeight, 5, 5);
            }
            gr.setColor(Color.BLACK);
            gr.drawRoundRect(rectX, rectY, rectWidth, rectHeight, 5, 5);

            gr.setFont(loadingTextFont);
            gr.drawString(loadingText, rectX + (rectWidth - loadingTextWidth) / 2, rectY + loadingTextHeight);

            if (currentMessage != null) {
                Font actionMessageFont = new Font("Dialog", Font.PLAIN, 10);
                gr.setFont(actionMessageFont);
                String[] splittedActionMessage = currentMessage.split(" ");
                String output = "";
                int maxWidth = messageRectWidth-10;
                int outputWidth = 0;
                int spaceWidth = gr.getFontMetrics(actionMessageFont).charWidth(' ');
                int wordWidth = 0;
                int j = 1;

                for (String word : splittedActionMessage) {
                    if (j <= 6) {
                        wordWidth = 0;
                        for (int i = 0; i < word.length(); i++)
                            wordWidth += gr.getFontMetrics(actionMessageFont).charWidth(word.charAt(i));
                        if ((outputWidth + spaceWidth + wordWidth) < maxWidth) {
                            output = output.concat(" ").concat(word);
                            outputWidth += spaceWidth + wordWidth;
                        } else {
                            gr.drawString(output, messageRectX + 5, messageRectY + 15 * j);
                            output = word;
                            outputWidth = wordWidth;
                            j = j + 1;
                        }
                    }
                }
                if (j <= 6)
                    gr.drawString(output, messageRectX + 5, messageRectY + 15 * j);
            }

            int tab = (count - segmentCount) % (maxSegmentCount + segmentCount);
            for (int j = 0; j < segmentCount; j++) {
                if ((j + tab - segmentCount) >= 0 && (j + tab - segmentCount) < maxSegmentCount) {
                    int alpha = 255 / segmentCount * (j + 1);
                    int segmentX = barX + (j + tab - segmentCount) * (segmentWidth + segmentGap);
                    gr.setColor(new Color(192, 192, 255, alpha));
                    gr.fillRoundRect(segmentX, barY, segmentWidth, segmentHeight, 3, 3);
                }
            }
        }
        count++;
    }

    public BufferedImage getScreen(Window window) {
        Rectangle screenRectangle = new Rectangle(window.getLocation(), window.getSize());
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        return robot.createScreenCapture(screenRectangle);
    }

    public BufferedImage blur(BufferedImage src) {
        float data[] = {0.125f, 0.125f, 0.0625f, 0.125f, 0.125f, 0.125f, 0.0625f, 0.125f, 0.125f};
        Kernel kernel = new Kernel(3, 3, data);
        ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return convolve.filter(src, null);
    }

    public BufferedImage darken(BufferedImage src) {
        float data[] = {0.5f};
        Kernel kernel = new Kernel(1, 1, data);
        ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return convolve.filter(src, null);
    }

}
