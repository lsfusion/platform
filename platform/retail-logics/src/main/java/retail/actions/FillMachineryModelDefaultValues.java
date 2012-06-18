package retail.actions;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.Message;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import retail.RetailBusinessLogics;
import retail.api.remote.SalesInfo;

import javax.swing.*;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Time;
import java.util.*;

public class FillMachineryModelDefaultValues extends ScriptingActionProperty {
    public FillMachineryModelDefaultValues(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        try {

            List<ModelInfo> modelInfoList = new ArrayList<ModelInfo>();
            modelInfoList.add(new ModelInfo("cashRegisterModel", "Кассы EasyCSV", "EasyCSVHandler"));
            modelInfoList.add(new ModelInfo("cashRegisterModel", "Кассы Kristal", "KristalHandler"));
            modelInfoList.add(new ModelInfo("cashRegisterModel", "Кассы Maxishop", "MaxishopHandler"));
            modelInfoList.add(new ModelInfo("cashRegisterModel", "Кассы UKM4", "UKM4Handler"));

            modelInfoList.add(new ModelInfo("scalesModel", "Весы Digi", "DigiHandler"));
            modelInfoList.add(new ModelInfo("scalesModel", "Весы EasyCSV", "EasyCSVHandler"));
            modelInfoList.add(new ModelInfo("scalesModel", "Весы Штрих-Принт", "ShtrihPrintHandler"));

            modelInfoList.add(new ModelInfo("priceCheckerModel", "Прайс-чекеры EasyCSV", "EasyCSVHandler"));

            modelInfoList.add(new ModelInfo("terminalModel", "ТСД InventoryTech", "InventoryTechHandler"));
            modelInfoList.add(new ModelInfo("terminalModel", "ТСД EasyCSV", "EasyCSVHandler"));

            DataSession session = context.getSession();

            for (ModelInfo modelInfo : modelInfoList) {
                DataObject modelObject = session.addObject((ConcreteCustomClass) getClass(modelInfo.className), session.modifier);
                getLP("name").execute(modelInfo.name, session, modelObject);
                getLP("handlerModel").execute(modelInfo.handler, session, modelObject);
                getLP("noteModel").execute("Создана автоматически", session, modelObject);
            }

            String result = session.apply(LM.getBL());
            if (result != null) {
                throw new RuntimeException("Набор моделей уже был добавлен ранее");
            }
            DataObject equServerObject = session.addObject((ConcreteCustomClass) getClass("equipmentServer"), session.modifier);
            getLP("name").execute("Equipment Server по умолчанию", session, equServerObject);
            getLP("sidEquipmentServer").execute("equServer1", session, equServerObject);

            session.apply(LM.getBL());

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private class ModelInfo {
        String className;
        String name;
        String handler;

        public ModelInfo(String className, String name, String handler) {
            this.className = className;
            this.name = name;
            this.handler = handler;
        }
    }
}