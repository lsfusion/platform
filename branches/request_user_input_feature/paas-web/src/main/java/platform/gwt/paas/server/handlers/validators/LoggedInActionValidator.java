package platform.gwt.paas.server.handlers.validators;

import com.gwtplatform.dispatch.server.spring.actionvalidator.DefaultActionValidator;
import com.gwtplatform.dispatch.shared.Action;
import com.gwtplatform.dispatch.shared.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class LoggedInActionValidator extends DefaultActionValidator {
    @Override
    public boolean isValid(Action<? extends Result> action) {
//        return true;
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }
}
