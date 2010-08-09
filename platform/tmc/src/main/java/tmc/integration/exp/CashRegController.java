package tmc.integration.exp;

import platform.interop.action.*;
import platform.server.classes.DataClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.view.form.*;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.form.client.RemoteFormView;
import platform.server.view.form.filter.NotNullFilter;
import platform.server.view.navigator.NavigatorElement;
import platform.server.view.navigator.NavigatorForm;
import platform.server.view.navigator.PropertyViewNavigator;
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

    public List<ClientAction> getCashRegApplyActions(RemoteForm remoteForm, int payType,
                                                      Set<GroupObjectImplement> propertyGroups, Set<GroupObjectImplement> classGroups,
                                                      PropertyViewNavigator<?> priceProp, PropertyViewNavigator<?> quantityProp,
                                                      PropertyViewNavigator<?> nameProp, PropertyViewNavigator<?> sumProp,
                                                      PropertyViewNavigator<?> toPayProp,
                                                      PropertyViewNavigator<?> sumCardProp, PropertyViewNavigator<?> sumCashProp) {

        List<ClientAction> actions = new ArrayList<ClientAction>();
        actions.add(new ExportFileClientAction("c:\\bill\\bill.txt", false, createBillTxt(remoteForm, payType,
                                                propertyGroups, classGroups, priceProp, quantityProp,
                                                nameProp, sumProp, toPayProp, sumCardProp, sumCashProp), CASHREGISTER_CHARSETNAME));
        actions.add(new ExportFileClientAction("c:\\bill\\key.txt", false, "/T", CASHREGISTER_CHARSETNAME));
        actions.add(new SleepClientAction(CASHREGISTER_DELAY));
        actions.add(new ImportFileClientAction(1, "c:\\bill\\error.txt", CASHREGISTER_CHARSETNAME, true));
        return actions;
    }

    private String createBillTxt(RemoteForm remoteForm, int payType,
                                 Set<GroupObjectImplement> propertyGroups, Set<GroupObjectImplement> classGroups,
                                 PropertyViewNavigator<?> priceProp, PropertyViewNavigator<?> quantityProp,
                                 PropertyViewNavigator<?> nameProp, PropertyViewNavigator<?> sumProp,
                                 PropertyViewNavigator<?> toPayProp,
                                 PropertyViewNavigator<?> sumCardProp, PropertyViewNavigator<?> sumCashProp) {

        String result = payType + ",0000\n";

        FormData data;

        PropertyView quantityView = remoteForm.mapper.mapPropertyView((PropertyViewNavigator<?>)quantityProp);
        quantityView.toDraw.addTempFilter(new NotNullFilter(quantityView.view));

        try {
            data = remoteForm.getFormData(propertyGroups, classGroups);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            quantityView.toDraw.clearTempFilters();
        }

        Double sumDoc = 0.0;

        for (FormRow row : data.rows) {

            Object quantityObject = row.values.get(remoteForm.mapper.mapPropertyView(quantityProp));

            if (quantityObject != null) {

                Double quantity = (quantityObject instanceof Double) ? (Double)quantityObject : 1.0;
                Double price = (Double)row.values.get(remoteForm.mapper.mapPropertyView(priceProp));
                String artName = ((String)row.values.get(remoteForm.mapper.mapPropertyView(nameProp))).trim();
                Double sumPos = (Double) row.values.get(remoteForm.mapper.mapPropertyView(sumProp));

                result += price / 100;
                result += ",0";
                result += "," + quantity;
                result += "," + artName;
                result += "," + sumPos / 100;
                result += "\n";

                sumDoc += price*quantity;
            }
        }

        Double toPay = (Double)data.rows.get(0).values.get(remoteForm.mapper.mapPropertyView(toPayProp));
        if (toPay == null) toPay = 0.0;
        Double sumDisc = sumDoc - toPay;
        if (sumDisc > 0) {
            result += "#," + sumDisc / 100 + "\n";
        }

        if (sumCardProp != null) {
            Double sumCard = (Double)data.rows.get(0).values.get(remoteForm.mapper.mapPropertyView(sumCardProp));
            if (sumCard != null && sumCard > 0) {
                result += "~1," + sumCard / 100 + "\n";
            }
        }

        Double sumCash;
        if (sumCashProp != null) {
            sumCash = (Double)data.rows.get(0).values.get(remoteForm.mapper.mapPropertyView(sumCashProp));
            if (sumCash == null) sumCash = toPay;
        } else
            sumCash = toPay;

        result += sumCash / 100 + "\n";

        return result;
    }

    public String checkCashRegApplyActions(int actionID, ClientActionResult result) {

        if (actionID == 1) {

            ImportFileClientActionResult impFileResult = ((ImportFileClientActionResult)result);

            if (impFileResult.fileExists) {
                return (impFileResult.fileContent.isEmpty()) ? "Произошла ошибка нижнего уровня ФР" : ("Ошибка при записи на фискальный регистратор :" + impFileResult.fileContent);
            }
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

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteFormView executeForm, Map<ClassPropertyInterface, PropertyObjectInterface> mapObjects) throws SQLException {

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

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteFormView executeForm, Map<ClassPropertyInterface, PropertyObjectInterface> mapObjects) throws SQLException {
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

    public NavigatorForm createCashRegManagementNavigatorForm(NavigatorElement parent, int iID) {
        return new CashRegManagementNavigatorForm(parent, iID);
    }

    private class CashRegManagementNavigatorForm extends NavigatorForm {

        private CashRegManagementNavigatorForm(NavigatorElement parent, int iID) {
            super(parent, iID, "Операции с ФР");
            addPropertyView(BL.properties, BL.cashRegGroup, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();
            design.setFont(VEDBusinessLogics.FONT_HUGE_BOLD, true);
            design.setPanelLabelAbove(BL.baseGroup, true);

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }
    }

}
