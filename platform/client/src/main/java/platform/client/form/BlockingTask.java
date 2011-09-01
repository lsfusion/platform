package platform.client.form;

import platform.client.ClientResourceBundle;
import platform.client.SwingUtils;
import platform.interop.RemoteContextInterface;
import platform.interop.remote.PendingRemote;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.rmi.RemoteException;
import java.util.TimerTask;

public class BlockingTask extends TimerTask {
    Image image;
    private boolean first = true;
    private int messagePeriod;
    PendingRemote target;

    public BlockingTask(PendingRemote target, int messagePeriod) {
        this.target = target;
        this.messagePeriod = messagePeriod;
    }

    @Override
    public void run() {
        Window window = SwingUtils.getActiveVisibleWindow();
        if (window != null) {
            Graphics gr = window.getGraphics();
            if (gr != null) {
                if (image == null) {
                //    image = blur(blur(getScreen(window)));
                    image = darken(getScreen(window));
                }
                if (first) {
                    gr.drawImage(image, 0, 0, null);
                    first = false;
                }
                try {
                    drawProgressBar(window);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int count = 0;
    private long previousTime = 0;
    private String actionMessage = null;

    private void drawProgressBar(Window window) throws RemoteException {
        Component canvas = null;
        if (window instanceof JDialog) {
            canvas = ((JDialog) window).getContentPane();
        } else if (window instanceof JFrame) {
            canvas = ((JFrame) window).getContentPane();
        }

        Graphics gr = window.getGraphics();
        if (gr != null && canvas != null) {
            Font loadingTextFont = new Font("Dialog", Font.BOLD, 15);
            Font actionMessageFont = new Font("Dialog", Font.PLAIN, 10);
            String loadingText = ClientResourceBundle.getString("form.loading");
            int segmentHeight = 25;
            int segmentWidth = 10;
            int segmentGap = 3;
            int segmentCount = (int) (canvas.getWidth() / (segmentWidth + segmentGap) * 0.4);

            int loadingTextWidth = gr.getFontMetrics(loadingTextFont).stringWidth(loadingText);
            int loadingTextHeight = gr.getFontMetrics(loadingTextFont).getHeight();

            int actionMessageHeight = gr.getFontMetrics(actionMessageFont).getHeight();
            long currentTime = System.currentTimeMillis();
            if (target instanceof RemoteContextInterface && (previousTime == 0 || currentTime - previousTime >= messagePeriod)) {
                actionMessage = ((RemoteContextInterface) target).getRemoteActionMessage();
                previousTime = currentTime;
            }

            int barWidth = segmentCount * segmentWidth + (segmentCount - 1) * (segmentGap);
            int rectHeight = segmentHeight + loadingTextHeight + actionMessageHeight + 23;
            int rectWidth = Math.max(barWidth, loadingTextWidth) + 20;
            int rectX = canvas.getX() + (canvas.getWidth() - Math.max(barWidth, loadingTextWidth)) / 2 - 10;
            int rectY = canvas.getY() + (canvas.getHeight()) / 2;

            if (loadingTextWidth > barWidth) {
                segmentCount = loadingTextWidth / (segmentWidth + segmentGap);
            }

            gr.setColor(Color.DARK_GRAY);
            gr.fillRoundRect(rectX, rectY, rectWidth + 2, rectHeight + 2, 5, 5);
            gr.setColor(Color.WHITE);
            gr.fillRoundRect(rectX, rectY, rectWidth, rectHeight, 5, 5);
            gr.setColor(Color.BLACK);
            gr.drawRoundRect(rectX, rectY, rectWidth, rectHeight, 5, 5);

            gr.setFont(loadingTextFont);
            gr.drawString(loadingText, rectX + (rectWidth - loadingTextWidth) / 2, rectY + loadingTextHeight);

            if (actionMessage != null) {
                gr.setFont(actionMessageFont);
                gr.drawString(actionMessage, rectX + 10, rectY + rectHeight - 10);
            }

            for (int i = 1; i <= count % (segmentCount + 1); i++) {
                int segmentX = rectX + 10 + (i - 1) * (segmentWidth + segmentGap);
                gr.setColor(Color.LIGHT_GRAY);
                gr.fillRoundRect(segmentX, rectY + loadingTextHeight + 10, segmentWidth + 2, segmentHeight + 2, 3, 3);
                gr.setColor(Color.DARK_GRAY);
                gr.fillRoundRect(rectX + 10 + (i - 1) * (segmentWidth + segmentGap), rectY + loadingTextHeight + 10, segmentWidth, segmentHeight, 3, 3);
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
