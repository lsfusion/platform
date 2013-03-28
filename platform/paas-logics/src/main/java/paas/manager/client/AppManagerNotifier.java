package paas.manager.client;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import paas.manager.common.NotificationData;
import platform.interop.DaemonThreadFactory;
import platform.server.ServerLoggers;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static platform.server.lifecycle.LifecycleEvent.*;

public class AppManagerNotifier extends LifecycleAdapter implements InitializingBean {
    private final static Logger logger = ServerLoggers.systemLogger;

    private ChannelFactory channelFactory;

    private String appManagerHost;

    private int appManagerPort;

    private int paasConfigurationId = -1;

    private ClientBootstrap bootstrap;

    public AppManagerNotifier() {
        super(LOGICS_ORDER - 1);
    }

    public void setAppManagerHost(String appManagerHost) {
        this.appManagerHost = appManagerHost;
    }

    public void setAppManagerPort(int appManagerPort) {
        this.appManagerPort = appManagerPort;
    }

    public void setPaasConfigurationId(int paasConfigurationId) {
        this.paasConfigurationId = paasConfigurationId;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(appManagerHost, "appManagerHost must be specified");
        Assert.state(paasConfigurationId > -1, "paasConfigurationId must be > 0");
        Assert.state(0 < appManagerPort && appManagerPort <= 65535, "appManagerPort must be between 0 and 65535");
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        channelFactory = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(new DaemonThreadFactory())
        );

        bootstrap = new ClientBootstrap(channelFactory);

        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);

        notifyRemoteAppManager(INIT);
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        notifyRemoteAppManager(STARTED);
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        notifyRemoteAppManager(STOPPING);
    }

    @Override
    protected void onStopped(LifecycleEvent event) {
        notifyRemoteAppManager(STOPPED);
        channelFactory.releaseExternalResources();
    }

    @Override
    protected void onError(LifecycleEvent event) {
        notifyRemoteAppManager(ERROR, (String) event.getData());
    }

    private void notifyRemoteAppManager(String eventType) {
        notifyRemoteAppManager(eventType, null);
    }

    private void notifyRemoteAppManager(String eventType, String message) {
        //todo: запускать в отдельном потоке и при ошибке подключения ждать пока не появится AppManager

        final NotificationData notificationDataToSend = new NotificationData(paasConfigurationId, eventType, message);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectEncoder(),
                        new AppManagerNotifierNotifyHandler(notificationDataToSend)
                );
            }
        });

        ChannelFuture future = bootstrap.connect(new InetSocketAddress(appManagerHost, appManagerPort));

        future.awaitUninterruptibly();
        if (!future.isSuccess()) {
            logger.error("Error connecting to manager application: ", future.getCause());
        }
        future.getChannel().getCloseFuture().awaitUninterruptibly();
    }
}
