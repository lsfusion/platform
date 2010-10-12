package tmc.integration.exp;

import platform.interop.action.*;
import platform.server.classes.DataClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.NotNullFilterInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.base.BaseUtils;
import tmc.VEDBusinessLogics;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CashRegController {

    private static String CASHREGISTER_CHARSETNAME = "Cp866";
    private static int CASHREGISTER_DELAY = 2000;

    VEDBusinessLogics BL;

    public CashRegController(VEDBusinessLogics BL) {
        this.BL = BL;
    }

    public ClientAction getCashRegApplyActions(FormInstance formInstance, int payType,
                                                      Set<GroupObjectInstance> classGroups,
                                                      PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                                      PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                                      PropertyDrawEntity<?> toPayProp,
                                                      PropertyDrawEntity<?> sumCardProp, PropertyDrawEntity<?> sumCashProp) {

        List<ClientAction> actions = new ArrayList<ClientAction>();
        actions.add(new ExportFileClientAction("c:\\bill\\bill.txt", false, createBillTxt(formInstance, payType,
                                                classGroups, priceProp, quantityProp,
                                                nameProp, sumProp, toPayProp, sumCardProp, sumCashProp), CASHREGISTER_CHARSETNAME));
        actions.add(new ExportFileClientAction("c:\\bill\\key.txt", false, "/T", CASHREGISTER_CHARSETNAME));
        actions.add(new SleepClientAction(CASHREGISTER_DELAY));
        actions.add(new ImportFileClientAction("c:\\bill\\error.txt", CASHREGISTER_CHARSETNAME, true));
        return new ListClientAction(actions);
    }

    private String createBillTxt(FormInstance formInstance, int payType,
                                 Set<GroupObjectInstance> classGroups,
                                 PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                 PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                 PropertyDrawEntity<?> toPayProp,
                                 PropertyDrawEntity<?> sumCardProp, PropertyDrawEntity<?> sumCashProp) {

        String result = payType + ",0000\n";

        FormData data;

        PropertyDrawInstance quantityDraw = formInstance.instanceFactory.getInstance(quantityProp);
        PropertyDrawInstance priceDraw = formInstance.instanceFactory.getInstance(priceProp);
        PropertyDrawInstance nameDraw = formInstance.instanceFactory.getInstance(nameProp);
        PropertyDrawInstance sumDraw = formInstance.instanceFactory.getInstance(sumProp);
        PropertyDrawInstance toPayDraw = formInstance.instanceFactory.getInstance(toPayProp);
        PropertyDrawInstance sumCardDraw = formInstance.instanceFactory.getInstance(sumCardProp);
        PropertyDrawInstance sumCashDraw = formInstance.instanceFactory.getInstance(sumCashProp);

        quantityDraw.toDraw.addTempFilter(new NotNullFilterInstance(quantityDraw.propertyObject));

        try {
            data = formInstance.getFormData(BaseUtils.toSetElements(quantityDraw, priceDraw, nameDraw, sumDraw, toPayDraw, sumCardDraw, sumCashDraw), classGroups);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            quantityDraw.toDraw.clearTempFilters();
        }

        Double sumDoc = 0.0;

        for (FormRow row : data.rows) {

            Object quantityObject = row.values.get(quantityDraw);

            if (quantityObject != null) {

                Object priceObject = row.values.get(priceDraw);
                
                Double quantity = (quantityObject instanceof Double) ? (Double)quantityObject : 1.0;
                Double price = (priceObject instanceof Double) ? (Double)priceObject : 0.0;
                String artName = ((String)row.values.get(nameDraw)).trim();
                artName = artName.replace(',', '.');
                artName = artName.replace('"', ' ');
                artName = artName.replace('\'', ' ');
                Double sumPos = (Double) row.values.get(sumDraw);

                result += price / 100;
                result += ",0";
                result += "," + quantity;
                result += "," + artName;
                result += "," + sumPos / 100;
                result += "\n";

                sumDoc += price*quantity;
            }
        }

        Double toPay = (Double)data.rows.get(0).values.get(toPayDraw);
        if (toPay == null) toPay = 0.0;
        Double sumDisc = sumDoc - toPay;
        if (sumDisc > 0) {
            result += "#," + sumDisc / 100 + "\n";
        }

        if (sumCardProp != null) {
            Double sumCard = (Double)data.rows.get(0).values.get(sumCardDraw);
            if (sumCard != null && sumCard > 0) {
                result += "~1," + sumCard / 100 + "\n";
            }
        }

        Double sumCash;
        if (sumCashProp != null) {
            sumCash = (Double)data.rows.get(0).values.get(sumCashDraw);
            if (sumCash == null) sumCash = toPay;
        } else
            sumCash = toPay;

        result += sumCash / 100 + "\n";

        return result;
    }

    public String checkCashRegApplyActions(Object result) {

        List<Object> listActions = (List<Object>) result;
        ImportFileClientActionResult impFileResult = ((ImportFileClientActionResult) listActions.get(listActions.size()-1));

        if (impFileResult.fileExists) {
            return (impFileResult.fileContent.isEmpty()) ? "Произошла ошибка нижнего уровня ФР" : ("Ошибка при записи на фискальный регистратор :" + impFileResult.fileContent);
        }

        return null;
    }

    public void addCashRegProperties() {

//       пока не поддерживается
//        BL.addProp(BL.cashRegOperGroup, new SimpleCashRegActionProperty(BL.genSID(), "Аннулировать чек", "/A")); 
        BL.addProp(BL.cashRegOperGroup, new SimpleCashRegActionProperty(BL.genSID(), "Продолжить печать", "/R"));
        BL.addProp(BL.cashRegOperGroup, new SimpleCashRegActionProperty(BL.genSID(), "Запрос наличных в денежном ящике", "/C", "cash.txt", 100));
        BL.addProp(BL.cashRegOperGroup, new SimpleCashRegActionProperty(BL.genSID(), "Открыть денежный ящик", "/O"));
        BL.addProp(BL.cashRegAdminGroup, new SimpleCashRegActionProperty(BL.genSID(), "Запрос номера последнего чека", "/N", "bill_no.txt"));
        BL.addProp(BL.cashRegAdminGroup, new IntegerCashRegActionProperty(BL.genSID(), "Внесение денег", "/P"));
        BL.addProp(BL.cashRegAdminGroup, new IntegerCashRegActionProperty(BL.genSID(), "Изъятие денег", "/G"));
        BL.addProp(BL.cashRegAdminGroup, new SimpleCashRegActionProperty(BL.genSID(), "X-отчет (сменный отчет без гашения)", "/X"));
        BL.addProp(BL.cashRegAdminGroup, new SimpleCashRegActionProperty(BL.genSID(), "Z-отчет (сменный отчет с гашением)", "/Z"));
        BL.addProp(BL.cashRegAdminGroup, new SimpleCashRegActionProperty(BL.genSID(), "Запрос серийного номера регистратора", "/S", "serial.txt"));
    }

    private class SimpleCashRegActionProperty extends ActionProperty {

        String command, outputFile;
        int multiplier;

        private SimpleCashRegActionProperty(String sID, String caption, String command) {
            this(sID, caption, command, null);
        }

        private SimpleCashRegActionProperty(String sID, String caption, String command, String outputFile) {
            this(sID, caption, command, outputFile, 0);
        }

        private SimpleCashRegActionProperty(String sID, String caption, String command, String outputFile, int multiplier) {
            super(sID, caption, new ValueClass[] {});
            this.command = command;
            this.outputFile = outputFile;
            this.multiplier = multiplier;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {

            actions.add(new ExportFileClientAction("c:\\bill\\key.txt", false, command, CASHREGISTER_CHARSETNAME));
            actions.add(new SleepClientAction(CASHREGISTER_DELAY));
            actions.add(new MessageFileClientAction("c:\\bill\\error.txt", CASHREGISTER_CHARSETNAME, false, true, caption));

            if (outputFile != null) {
                actions.add(new MessageFileClientAction("c:\\bill\\" + outputFile, CASHREGISTER_CHARSETNAME, true, true, caption, multiplier));
            }
        }
    }

    private class IntegerCashRegActionProperty extends ActionProperty {

        String command;

        private IntegerCashRegActionProperty(String sID, String caption, String command) {
            super(sID, caption, new ValueClass[] {});
            this.command = command;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            if (value.getValue() != null && value.getValue() instanceof Double) {
                actions.add(new ExportFileClientAction("c:\\bill\\key.txt", false, command + ":" + (Double)value.getValue()/100, CASHREGISTER_CHARSETNAME));
                actions.add(new SleepClientAction(CASHREGISTER_DELAY));
                actions.add(new MessageFileClientAction("c:\\bill\\error.txt", CASHREGISTER_CHARSETNAME, false, true, caption));
            }
        }

        @Override
        protected DataClass getValueClass() {
            return DoubleClass.instance;
        }
    }

    public FormEntity createCashRegManagementFormEntity(NavigatorElement parent, int iID) {
        return new CashRegManagementFormEntity(parent, iID);
    }

    private class CashRegManagementFormEntity extends FormEntity {

        private CashRegManagementFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Операции с ФР");
            addPropertyDraw(BL.cashRegGroup, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();
            design.setFont(VEDBusinessLogics.FONT_HUGE_BOLD);
            design.setPanelLabelAbove(BL.baseGroup, true);

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }
    }

}
