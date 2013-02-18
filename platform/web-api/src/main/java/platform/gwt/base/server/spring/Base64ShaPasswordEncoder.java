package platform.gwt.base.server.spring;

import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import platform.base.BaseUtils;

public class Base64ShaPasswordEncoder extends ShaPasswordEncoder {

    public Base64ShaPasswordEncoder() {
        this(256);
    }

    public Base64ShaPasswordEncoder(int strength) {
        super(strength);
    }

    @Override
    public String encodePassword(String rawPass, Object salt) {
        return BaseUtils.calculateBase64Hash("SHA-256", rawPass, salt);
    }
}
