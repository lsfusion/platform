package platform.gwt.paas.server.spring;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.base.server.spring.InvalidateListener;
import platform.gwt.base.server.spring.SingleBusinessLogicsProvider;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ConfigurationBLProviderImpl implements ConfigurationBLProvider, InitializingBean {
    private final int LOGICS_EXPIRATION_MINUTES = 10;

    private final ThreadLocal<BusinessLogicsProvider> threadLocalProviders = new ThreadLocal<BusinessLogicsProvider>();

    private final LoadingCache<Integer, BusinessLogicsProvider> configurationBLProviders = CacheBuilder.newBuilder()
            .expireAfterAccess(LOGICS_EXPIRATION_MINUTES, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<Integer, BusinessLogicsProvider>() {
                        public BusinessLogicsProvider load(Integer configurationId) throws RemoteException {
                            ConfigurationDTO configuration = paasProvider.getLogics().getConfiguration(null, configurationId);
                            return new SingleBusinessLogicsProvider(serverHost, configuration.port);
                        }
                    }
            );

    private String serverHost;

    private BusinessLogicsProvider<PaasRemoteInterface> paasProvider;

    public void setPaasProvider(BusinessLogicsProvider<PaasRemoteInterface> paasProvider) {
        this.paasProvider = paasProvider;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(serverHost, "serverHost must be set");
        Assert.notNull(paasProvider, "paasProvider must be set");
    }

    //todo: нужно удалять закэшированную логику, если порт конфигурации изменился
    //todo: хотя нужно бы сделать, чтобы это можно было делать только через стоп логики
    //todo: при этом нужно автоматом удалять логику из кэша
    public void initCurrentProvider(int configurationId) throws IOException {
        try {
            BusinessLogicsProvider blProvider = configurationBLProviders.get(configurationId);

            //todo: cache timeZone
            ServerUtils.timeZone.set(blProvider.getLogics().getTimeZone());
            threadLocalProviders.set(blProvider);
        } catch (ExecutionException e) {
            Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
            Throwables.propagate(e.getCause());
        }
    }

    public void setCurrentProviderToPaas() {
        threadLocalProviders.set(paasProvider);
    }

    public BusinessLogicsProvider getCurrentProvider() {
        return threadLocalProviders.get();
    }

    @Override
    public RemoteLogicsInterface getLogics() {
        return getCurrentProvider().getLogics();
    }

    @Override
    public void invalidate() {
        getCurrentProvider().invalidate();
    }

    @Override
    public void addInvlidateListener(InvalidateListener listener) {
        getCurrentProvider().addInvlidateListener(listener);
    }

    @Override
    public void removeInvlidateListener(InvalidateListener listener) {
        getCurrentProvider().removeInvlidateListener(listener);
    }
}
