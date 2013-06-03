package lsfusion.agent;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.util.CharsetUtil;
import lsfusion.interop.DaemonThreadFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class ServerAgentLocator {
    private final int acceptPort;
    private final int agentPort;

    public ServerAgentLocator(int acceptPort, int agentPort) {
        this.acceptPort = acceptPort;
        this.agentPort = agentPort;
    }

    public void start() {
        DatagramChannelFactory dcf = new NioDatagramChannelFactory(Executors.newCachedThreadPool(new DaemonThreadFactory("-server-locator-daemon")));
        ConnectionlessBootstrap cb = new ConnectionlessBootstrap(dcf);


        // Configure the pipeline factory.
        cb.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectEncoder(),
                        new StringDecoder(CharsetUtil.UTF_8),
                        new ServerAgentLocatorHandler(agentPort));
            }
        });

        cb.setOption("broadcast", "false");
        cb.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));

        // Bind to the port and start the service.
        cb.bind(new InetSocketAddress(acceptPort));
    }

    //    Performance optimization:
//    Если надо, то можно использовать броадкаст для группы, а не для всех компов в сети.
//
//    public void start() throws UnknownHostException {
//        DatagramChannelFactory dcf = new OioDatagramChannelFactory(Executors.newCachedThreadPool());
//        ConnectionlessBootstrap cb = new ConnectionlessBootstrap(dcf);
//
//        // Configure the pipeline factory.
//        cb.setPipelineFactory(new ChannelPipelineFactory() {
//            public ChannelPipeline getPipeline() throws Exception {
//                return Channels.pipeline(
//                        new ObjectEncoder(),
//                        new StringDecoder(CharsetUtil.UTF_8),
//                        new ServerAgentLocatorHandler(exportPort));
//            }
//        });
//
//        cb.setOption("broadcast", "false");
//        cb.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));
//
//        // Bind to the port and start the service.
//        InetSocketAddress multicastAddress = new InetSocketAddress(6666);
//        DatagramChannel datagramChannel = (DatagramChannel) cb.bind(multicastAddress);
//        datagramChannel.joinGroup(InetAddress.getByName("230.0.0.1"));
//    }
}
