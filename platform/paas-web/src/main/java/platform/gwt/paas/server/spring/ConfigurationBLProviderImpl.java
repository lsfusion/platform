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
                            return new SingleBusinessLogicsProvider(registryHost, registryPort, configuration.exportName);
                        }
                    }
            );

    private BusinessLogicsProvider<PaasRemoteInterface> paasProvider;

    private String registryHost;
    private int registryPort;

    public void setPaasProvider(BusinessLogicsProvider<PaasRemoteInterface> paasProvider) {
        this.paasProvider = paasProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(paasProvider, "paasProvider must be set");

        registryHost = paasProvider.getRegistryHost();
        registryPort = paasProvider.getRegistryPort();
    }

    //todo: нужно удалять закэшированную логику, если порт конфигурации изменился
    //todo: хотя нужно бы сделать, чтобы это можно было делать только через стоп логики
    //todo: при этом нужно автоматом удалять логику из кэша
    public void initCurrentProvider(int configurationId) throws IOException {
        try {
            BusinessLogicsProvider blProvider = configurationBLProviders.get(configurationId);

            try {
                //todo: cache timeZone
                ServerUtils.timeZone.set(blProvider.getLogics().getTimeZone());
            } catch (RemoteException e) {
                blProvider.invalidate();
                throw e;
            }
            threadLocalProviders.set(blProvider);
        } catch (ExecutionException e) {
            Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
            Throwables.propagate(e.getCause());
        }
    }

    public void setCurrentProviderToPaas() {
        threadLocalProviders.set(paasProvider);
    }

    public String getRegistryHost() {
        return registryHost;
    }

    public int getRegistryPort() {
        return registryPort;
    }

    @Override
    public String getExportName() {
        return getCurrentProvider().getExportName();
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
    public void addInvalidateListener(InvalidateListener listener) {
        getCurrentProvider().addInvalidateListener(listener);
    }

    @Override
    public void removeInvalidateListener(InvalidateListener listener) {
        getCurrentProvider().removeInvalidateListener(listener);
    }
}
