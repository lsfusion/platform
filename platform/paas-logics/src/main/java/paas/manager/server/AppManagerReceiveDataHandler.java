package paas.manager.server;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.*;
import paas.manager.common.NotificationData;

public final class AppManagerReceiveDataHandler extends SimpleChannelHandler {
    private final static Logger logger = Logger.getLogger(AppManagerReceiveDataHandler.class);

    private final AppManager applicationManager;

    public AppManagerReceiveDataHandler(AppManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        applicationManager.addOpenedChannel(e.getChannel());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        NotificationData notificationData = (NotificationData) e.getMessage();
        applicationManager.notificationReceived(notificationData);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.error("Error while performing some I/O in application manager.", e.getCause());
        e.getChannel().close();
    }
}
