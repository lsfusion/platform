package platform.gwt.paas.server.handlers.validators;

import com.gwtplatform.dispatch.server.actionvalidator.AbstractDefaultActionValidator;
import com.gwtplatform.dispatch.shared.Action;
import com.gwtplatform.dispatch.shared.Result;
import org.springframework.stereotype.Component;

@Component
public class AllowActionValidator extends AbstractDefaultActionValidator {
    @Override
    public boolean isValid(Action<? extends Result> action) {
        return true;
    }
}
