package lsfusion.agent;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import lsfusion.interop.ServerInfo;

import java.net.InetAddress;

public class ServerAgentLocatorHandler extends SimpleChannelUpstreamHandler {
    private final int agentPort;

    public ServerAgentLocatorHandler(int agentPort) {
        this.agentPort = agentPort;
    }

    //@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        String msg = (String)e.getMessage();
        if (msg.equals("give me server info, please.")) {
            InetAddress localHost = InetAddress.getLocalHost();
            ServerInfo serverInfo = new ServerInfo(localHost.getHostName(), localHost.getHostAddress(), agentPort);
            e.getChannel().write(serverInfo, e.getRemoteAddress());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
    }
}
