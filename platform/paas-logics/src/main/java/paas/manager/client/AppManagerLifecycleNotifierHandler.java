package paas.manager.client;

import org.jboss.netty.channel.*;
import platform.server.lifecycle.LifecycleEvent;

public final class AppManagerLifecycleNotifierHandler extends SimpleChannelHandler {
    private final LifecycleEvent event;

    public AppManagerLifecycleNotifierHandler(LifecycleEvent event) {
        this.event = event;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ChannelFuture future = e.getChannel().write(event);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
