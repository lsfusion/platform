package paas.manager.server;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.*;
import platform.server.lifecycle.LifecycleEvent;

public final class AppManagerChannelHandler extends SimpleChannelHandler {
    private final static Logger logger = Logger.getLogger(AppManagerChannelHandler.class);

    private final AppManager applicationManager;

    public AppManagerChannelHandler(AppManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        applicationManager.addOpenedChannel(e.getChannel());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        LifecycleEvent lifecycleEvent = (LifecycleEvent) e.getMessage();
        applicationManager.lifecycleEvent(lifecycleEvent);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.error("Error while performing some I/O in application manager.", e.getCause());
        e.getChannel().close();
    }
}
