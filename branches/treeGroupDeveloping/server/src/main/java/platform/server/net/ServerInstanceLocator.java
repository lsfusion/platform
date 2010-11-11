package platform.server.net;

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

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerInstanceLocator {

    public void start(ServerInstanceLocatorSettings settings, final int exportPort) throws UnknownHostException {
        DatagramChannelFactory dcf = new NioDatagramChannelFactory(Executors.newCachedThreadPool(new DaemonThreadFactory()));
        ConnectionlessBootstrap cb = new ConnectionlessBootstrap(dcf);

        // Configure the pipeline factory.
        cb.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectEncoder(),
                        new StringDecoder(CharsetUtil.UTF_8),
                        new ServerInstanceLocatorHandler(exportPort));
            }
        });

        cb.setOption("broadcast", "false");
        cb.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));

        // Bind to the port and start the service.
        cb.bind(new InetSocketAddress(settings.getPort()));
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DaemonThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                          poolNumber.getAndIncrement() +
                         "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (!t.isDaemon())
                t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
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
//                        new ServerInstanceLocatorHandler(exportPort));
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
