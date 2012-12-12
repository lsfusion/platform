package platform.server.logics;

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
import platform.server.session.DataSession;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.Arrays;

import static platform.server.logics.ServerResourceBundle.getString;

public class ServiceLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    Logger logger;
    T BL;

    public T getBL(){
        return BL;
    }

    private LAP checkAggregationsAction;
    private LAP recalculateAction;
    private LAP recalculateFollowsAction;
    private LAP analyzeDBAction;
    private LAP packAction;
    private LAP serviceDBAction;

    public ServiceLogicsModule(T BL, BaseLogicsModule baseLM, Logger logger) {
        super("Service", "Service");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
        this.logger = logger;
    }
    @Override
    public void initModuleDependencies() {
        setRequiredModules(Arrays.asList("System"));
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        initBaseClassAliases();
    }

    @Override
    public void initGroups() {
        initBaseGroupAliases();
    }

    @Override
    public void initTables() {

    }

    @Override
    public void initProperties() {
        // Управление сервером базы данных
        // todo : правильно разрисовать контейнеры
        checkAggregationsAction = addProperty(null, new LAP(new CheckAggregationsActionProperty("checkAggregationsAction", getString("logics.check.aggregations"))));
        recalculateAction = addProperty(null, new LAP(new RecalculateActionProperty("recalculateAction", getString("logics.recalculate.aggregations"))));
        recalculateFollowsAction = addProperty(null, new LAP(new RecalculateFollowsActionProperty("recalculateFollowsAction", getString("logics.recalculate.follows"))));
        analyzeDBAction = addProperty(null, new LAP(new AnalyzeDBActionProperty("analyzeDBAction", getString("logics.vacuum.analyze"))));
        packAction = addProperty(null, new LAP(new PackActionProperty("packAction", getString("logics.tables.pack"))));
        serviceDBAction = addProperty(null, new LAP(new ServiceDBActionProperty("serviceDBAction", getString("logics.service.db"))));


        initNavigators();
    }

    private void initNavigators() {
        addFormEntity(new AdminFormEntity(baseLM.configuration, "adminForm"));
    }
    
    @Override
    public void initIndexes() {
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    private class CheckAggregationsActionProperty extends AdminActionProperty {
        private CheckAggregationsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            String message = BL.checkAggregations(sqlSession);
            sqlSession.commitTransaction();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.check.aggregation.was.completed") + '\n' + '\n' + message, getString("logics.checking.aggregations"), true));
        }
    }

    private class RecalculateActionProperty extends AdminActionProperty {
        private RecalculateActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.recalculateAggregations(sqlSession, BL.getAggregateStoredProperties());
            sqlSession.commitTransaction();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
        }
    }

    private class RecalculateFollowsActionProperty extends AdminActionProperty {
        private RecalculateFollowsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataSession session = BL.createSession();
            BL.recalculateFollows(session);
            session.apply(BL);
            session.close();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.follows")));
        }
    }

    private class AnalyzeDBActionProperty extends AdminActionProperty {
        private AnalyzeDBActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataSession session = BL.createSession();
            BL.analyzeDB(session.sql);
            session.apply(BL);
            session.close();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.vacuum.analyze.was.completed"), getString("logics.vacuum.analyze")));
        }
    }

    private class PackActionProperty extends AdminActionProperty {
        private PackActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.packTables(sqlSession, baseLM.tableFactory.getImplementTables());
            sqlSession.commitTransaction();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.tables.packing.completed"), getString("logics.tables.packing")));
        }
    }

    private class ServiceDBActionProperty extends AdminActionProperty {
        private ServiceDBActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.recalculateAggregations(sqlSession, BL.getAggregateStoredProperties());
            sqlSession.commitTransaction();

            BL.recalculateFollows(context.getSession());

            sqlSession.startTransaction();
            BL.packTables(sqlSession, baseLM.tableFactory.getImplementTables());
            sqlSession.commitTransaction();

            BL.analyzeDB(sqlSession);

            BL.recalculateStats(context.getSession());
            context.getSession().apply(BL);

            context.delayUserInterfaction(new MessageClientAction(getString("logics.service.db.completed"), getString("logics.service.db")));
        }
    }

    private class AdminFormEntity extends FormEntity {
        private AdminFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.global.parameters"));

            addPropertyDraw(new LP[]{baseLM.defaultBackgroundColor,
                    baseLM.defaultForegroundColor, baseLM.restartServerAction, baseLM.cancelRestartServerAction, checkAggregationsAction, recalculateAction,
                    recalculateFollowsAction, packAction, analyzeDBAction, serviceDBAction, baseLM.runGarbageCollector});
        }
    }
}
