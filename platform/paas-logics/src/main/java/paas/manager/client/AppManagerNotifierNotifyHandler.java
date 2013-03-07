package paas.manager.client;

import org.jboss.netty.channel.*;
import paas.manager.common.NotificationData;

public final class AppManagerNotifierNotifyHandler extends SimpleChannelHandler {
    private final NotificationData notificationData;

    public AppManagerNotifierNotifyHandler(NotificationData notificationData) {
        this.notificationData = notificationData;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ChannelFuture future = e.getChannel().write(notificationData);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
