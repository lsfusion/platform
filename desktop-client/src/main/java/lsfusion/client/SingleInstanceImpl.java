package lsfusion.client;

import org.apache.log4j.Logger;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;

public class SingleInstanceImpl implements SingleInstance {

    static SingleInstanceService sis;
    static SingleInstanceListener sisL;
    private final static Logger logger = Logger.getLogger(Main.class);

    @Override
    public void register() {
        try {
            sis = (SingleInstanceService) ServiceManager.lookup("javax.jnlp.SingleInstanceService");
        } catch (UnavailableServiceException e) {
            sis = null;
        }
        if (sis != null) {
            sisL = new SingleInstanceListener() {
                @Override
                public void newActivation(String[] strings) {
                    logger.error("Attempt of running one more client");
                }
            };
            sis.addSingleInstanceListener(sisL);
        }
    }

    @Override
    public void unregister() {
        if(sis != null && sisL != null)
            sis.removeSingleInstanceListener(sisL);
    }
}
