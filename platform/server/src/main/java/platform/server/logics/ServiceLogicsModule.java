package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import platform.interop.Compare;
import platform.interop.PropertyEditType;
import platform.interop.action.MessageClientAction;
import platform.interop.form.layout.ContainerType;
import platform.server.classes.*;
import platform.server.data.SQLSession;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.AdminActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import static platform.server.logics.ServerResourceBundle.getString;

public class ServiceLogicsModule extends ScriptingLogicsModule {

    private LAP checkAggregationsAction;
    private LAP recalculateAction;
    private LAP recalculateFollowsAction;
    private LAP analyzeDBAction;
    private LAP packAction;
    private LAP serviceDBAction;

    public ServiceLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ServiceLogicsModule.class.getResourceAsStream("/scripts/Service.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();
        // Управление сервером базы данных
        checkAggregationsAction = getLAPByName("checkAggregationsAction");
        recalculateAction = getLAPByName("recalculateAction");
        recalculateFollowsAction = getLAPByName("recalculateFollowsAction");
        analyzeDBAction = getLAPByName("analyzeDBAction");
        packAction = getLAPByName("packAction");
        serviceDBAction = getLAPByName("serviceDBAction");
    }
}
