package lsfusion.gwt.form.client.dispatch;

import lsfusion.gwt.base.server.spring.BusinessLogicsProvider;
import lsfusion.gwt.form.server.FileUtils;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

public class CustomLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    BusinessLogicsProvider blProvider;

    public CustomLoginUrlAuthenticationEntryPoint(BusinessLogicsProvider blProvider) {
        this.blProvider = blProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        byte[] logo = blProvider.getLogics().getGUIPreferences().logicsLogo;
        if(logo != null) {
            FileUtils.saveFileInCurrentDir("logo.jpg", logo);
        } else
            FileUtils.copyFile("splash.jpg", "logo.jpg");
    }
}