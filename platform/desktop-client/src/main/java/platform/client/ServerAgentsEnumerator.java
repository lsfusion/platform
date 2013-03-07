package platform.client;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.util.CharsetUtil;
import platform.interop.DaemonThreadFactory;
import platform.interop.ServerInfo;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class ServerAgentsEnumerator extends SwingWorker<Void, ServerInfo> {
    private MutableComboBoxModel serverHostModel;
    private String waitMessage;

    public ServerAgentsEnumerator(MutableComboBoxModel serverHostModel, String waitMessage) {
        this.serverHostModel = serverHostModel;
        this.waitMessage = waitMessage;
    }

    @Override
    protected Void doInBackground() throws Exception {
        serverHostModel.addElement(waitMessage);

        DatagramChannelFactory dcf = new NioDatagramChannelFactory(Executors.newCachedThreadPool(new DaemonThreadFactory()));
        ConnectionlessBootstrap cb = new ConnectionlessBootstrap(dcf);

        cb.setPipelineFactory(new ChannelPipelineFactory() {

            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new StringEncoder(CharsetUtil.UTF_8),
                        new ObjectDecoder(),
                        new ServerInfoClientHandler());
            }
        });

        cb.setOption("broadcast", "true");

        cb.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));

        DatagramChannel dc = (DatagramChannel) cb.bind(new InetSocketAddress(0));

        String[] broadcastPorts = System.getProperty("platform.client.broadcastPorts", "6666,6667-6669").split(",");
        for (String port : broadcastPorts) {
            int pos = port.indexOf('-');
            if (pos == -1) {
                sendBroadcast(dc, Integer.parseInt(port));
            } else {
                int startPort = Integer.parseInt(port.substring(0, pos));
                int endPort = Integer.parseInt(port.substring(pos + 1));
                for (int curPort = startPort; curPort <= endPort; ++curPort) {
                    sendBroadcast(dc, curPort);
                }
            }
        }

        if (!dc.getCloseFuture().awaitUninterruptibly(1000)) {
            dc.close().awaitUninterruptibly();
        }

        dcf.releaseExternalResources();

        return null;
    }

    private void sendBroadcast(DatagramChannel dc, int broadcastPort) {
        dc.write("give me server info, please.", new InetSocketAddress("255.255.255.255", broadcastPort));
    }

    @Override
    protected void process(List<ServerInfo> chunks) {
        for (ServerInfo serverInfo : chunks) {
            serverHostModel.addElement(serverInfo);
        }
    }

    @Override
    protected void done() {
        serverHostModel.removeElement(waitMessage);
    }

    private class ServerInfoClientHandler extends SimpleChannelUpstreamHandler {

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Object msg = e.getMessage();
            if (msg instanceof ServerInfo) {
                ServerInfo serverInfo = (ServerInfo) msg;
                publish(serverInfo);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            e.getCause().printStackTrace();
            e.getChannel().close();
        }
    }
}
