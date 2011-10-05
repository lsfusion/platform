package paas.manager.client;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import paas.manager.common.ConfigurationEventData;
import platform.interop.DaemonThreadFactory;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.lifecycle.LifecycleListener;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static platform.server.lifecycle.LifecycleEvent.*;

public class AppManagerLifecycleNotifier implements LifecycleListener {
    private ChannelFactory channelFactory;

    private int managerPort;
    private String managerHost;
    private int configurationId;
    private ClientBootstrap bootstrap;

    public AppManagerLifecycleNotifier() {
        try {
            managerPort = Integer.parseInt(System.getProperty("paas.manager.port", "-1"));
            configurationId = Integer.parseInt(System.getProperty("paas.manager.conf.id", "-1"));


        } catch (NumberFormatException ignore) {
        }

        managerHost = System.getProperty("paas.manager.host", "localhost");

        if (managerPort<0 || managerPort>65535) {
            throw new IllegalStateException("System property paas.manager.port should be set to correct port number between 0 and 65536");
        }

        if (configurationId < 0) {
            throw new IllegalStateException("System property paas.manager.conf.id should be number > 0");
        }

        channelFactory = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(new DaemonThreadFactory())
        );

        bootstrap = new ClientBootstrap(channelFactory);

        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
    }

    @Override
    public synchronized void lifecycleEvent(LifecycleEvent event) {
        if (LOGICS_CREATED.equals(event.getType())) {
            return;
        }

        final LifecycleEvent eventToSent = new LifecycleEvent(event.getType(), new ConfigurationEventData(configurationId, event.getData()));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectEncoder(),
                        new AppManagerLifecycleNotifierHandler(eventToSent)
                );
            }
        });

        ChannelFuture future = bootstrap.connect(new InetSocketAddress(managerHost, managerPort));

        future.awaitUninterruptibly();
        if (!future.isSuccess()) {
            future.getCause().printStackTrace();
        }
        future.getChannel().getCloseFuture().awaitUninterruptibly();

        if (event.getType().equals(STOPPED)) {
            channelFactory.releaseExternalResources();
        }
    }
}
