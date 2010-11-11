package platform.server.net;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import platform.interop.ServerInfo;

import java.net.InetAddress;

public class ServerInstanceLocatorHandler extends SimpleChannelUpstreamHandler {
    private int exportPort;

    public ServerInstanceLocatorHandler(int exportPort) {
        this.exportPort = exportPort;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        String msg = (String)e.getMessage();
        if (msg.equals("give me server info, please.")) {
            InetAddress localHost = InetAddress.getLocalHost();
            ServerInfo serverInfo = new ServerInfo(localHost.getHostName(), localHost.getHostAddress(), exportPort);
            e.getChannel().write(serverInfo, e.getRemoteAddress());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
    }
}
