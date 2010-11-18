package tmc.integration.exp;

import platform.base.BaseUtils;
import platform.interop.action.*;
import platform.server.classes.DataClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.NotNullFilterInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import tmc.VEDBusinessLogics;
import tmc.integration.exp.FiscalRegistar.*;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;

public class CashRegController {

    private static String CASHREGISTER_CHARSETNAME = "Cp866";
    private static int CASHREGISTER_DELAY = 2000;

    VEDBusinessLogics BL;

    public CashRegController(VEDBusinessLogics BL) {
        this.BL = BL;
    }

    private boolean noBillTxt = false;
    private int cashRegComPort = 0;

    public int getCashRegComPort(FormInstance formInstance) {
        int result;
        try {
            result = (Integer) BL.cashRegComPort.read(formInstance.session, formInstance, formInstance.instanceFactory.computer.getDataObject());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            result = 0;
        }
        return result;
    }

    public ClientResultAction getCashRegApplyActions(FormInstance formInstance, int payType,
                                                     Set<GroupObjectInstance> classGroups,
                                                     PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                                     PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                                     PropertyDrawEntity<?> toPayProp, PropertyDrawEntity<?> barcodeProp,
                                                     PropertyDrawEntity<?> sumCardProp, PropertyDrawEntity<?> sumCashProp,
                                                     PropertyDrawEntity<?> orderArticleSaleDiscount, PropertyDrawEntity<?> orderArticleSaleDiscountSum,
                                                     PropertyDrawEntity<?> cashierProp, PropertyDrawEntity<?> clientNameProp,
                                                     PropertyDrawEntity<?> clientSumProp) {

        List<ClientResultAction> actions = new ArrayList<ClientResultAction>();

        cashRegComPort = getCashRegComPort(formInstance);
        if (cashRegComPort > 0) {
            actions.add(new CashRegPrintReceiptAction(payType, cashRegComPort, createReceipt(formInstance, payType,
                    classGroups, priceProp, quantityProp, nameProp,
                    sumProp, toPayProp, barcodeProp, sumCardProp, sumCashProp,
                    orderArticleSaleDiscount, orderArticleSaleDiscountSum, cashierProp, clientNameProp, clientSumProp)));

        }
        return new ListClientResultAction(actions);
    }


    public ReceiptInstance createReceipt(FormInstance formInstance, int payType,
                                         Set<GroupObjectInstance> classGroups,
                                         PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                         PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                         PropertyDrawEntity<?> toPayProp, PropertyDrawEntity<?> barcodeProp,
                                         PropertyDrawEntity<?> sumCardProp, PropertyDrawEntity<?> sumCashProp,
                                         PropertyDrawEntity<?> orderArticleSaleDiscountProp,
                                         PropertyDrawEntity<?> orderArticleSaleDiscountSumProp, PropertyDrawEntity<?> cashierProp,
                                         PropertyDrawEntity<?> clientNameProp, PropertyDrawEntity<?> clientSumProp) {

        ReceiptInstance result = new ReceiptInstance(payType);
        FormData data;

        Set<PropertyDrawInstance> formProperties = new HashSet<PropertyDrawInstance>();
        PropertyDrawInstance quantityDraw = formInstance.instanceFactory.getInstance(quantityProp);
        PropertyDrawInstance priceDraw = formInstance.instanceFactory.getInstance(priceProp);
        PropertyDrawInstance nameDraw = formInstance.instanceFactory.getInstance(nameProp);
        PropertyDrawInstance sumDraw = formInstance.instanceFactory.getInstance(sumProp);
        PropertyDrawInstance toPayDraw = formInstance.instanceFactory.getInstance(toPayProp);
        PropertyDrawInstance barcodeDraw = formInstance.instanceFactory.getInstance(barcodeProp);
        PropertyDrawInstance cashierNameDraw = formInstance.instanceFactory.getInstance(cashierProp);

        formProperties.addAll(BaseUtils.toSet(quantityDraw, priceDraw, nameDraw, sumDraw, toPayDraw,
                barcodeDraw, cashierNameDraw));

        PropertyDrawInstance clientNameDraw = null;
        if (clientNameProp != null) {
            clientNameDraw = formInstance.instanceFactory.getInstance(clientNameProp);
            formProperties.add(clientNameDraw);
        }

        PropertyDrawInstance clientSumDraw = null;
        if (clientSumProp != null) {
            clientSumDraw = formInstance.instanceFactory.getInstance(clientSumProp);
            formProperties.add(clientSumDraw);
        }

        PropertyDrawInstance sumCardDraw = null;
        if (sumCardProp != null) {
            sumCardDraw = formInstance.instanceFactory.getInstance(sumCardProp);
            formProperties.add(sumCardDraw);
        }

        PropertyDrawInstance sumCashDraw = null;
        if (sumCashProp != null) {
            sumCashDraw = formInstance.instanceFactory.getInstance(sumCashProp);
            formProperties.add(sumCashDraw);
        }

        PropertyDrawInstance orderArticleDiscountDraw = null;
        if (orderArticleSaleDiscountProp != null) {
            orderArticleDiscountDraw = formInstance.instanceFactory.getInstance(orderArticleSaleDiscountProp);
            formProperties.add(orderArticleDiscountDraw);
        }

        PropertyDrawInstance orderArticleDiscountSumDraw = null;
        if (orderArticleSaleDiscountSumProp != null) {
            orderArticleDiscountSumDraw = formInstance.instanceFactory.getInstance(orderArticleSaleDiscountSumProp);
            formProperties.add(orderArticleDiscountSumDraw);
        }

        quantityDraw.toDraw.addTempFilter(new NotNullFilterInstance(quantityDraw.propertyObject));

        try {
            data = formInstance.getFormData(formProperties, classGroups);
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

                Double quantity = (quantityObject instanceof Double) ? (Double) quantityObject : 1.0;
                Double price = (priceObject instanceof Double) ? (Double) priceObject : 0.0;
                String barcodeName = ((String) row.values.get(barcodeDraw)).trim();
                String artName = ((String) row.values.get(nameDraw)).trim();
                Double sumPos = (Double) row.values.get(sumDraw);
                Double articleDisc = (Double) row.values.get(orderArticleDiscountDraw);
                Integer articleDiscSum = (Integer) row.values.get(orderArticleDiscountSumDraw);

                result.add(new ReceiptItem(price, quantity, barcodeName, artName, sumPos,
                        articleDisc == null ? 0.0 : articleDisc, articleDiscSum == null ? 0 : articleDiscSum));

                sumDoc += price * quantity;
            }
        }

        Double toPay = (Double) data.rows.get(0).values.get(toPayDraw);
        if (toPay == null) toPay = 0.0;
        Double sumDisc = sumDoc - toPay;

        result.sumDisc = sumDisc;

        Double sumCard = null;
        if (sumCardProp != null) {
            sumCard = (Double) data.rows.get(0).values.get(sumCardDraw);
            if (sumCard != null && sumCard > 0) {
                result.sumCard = sumCard;
            }
        }
        if (sumCard == null) {
            sumCard = 0.0;
            result.sumCard = 0.0;
        }

        Double sumCash;
        if (sumCashProp != null) {
            sumCash = (Double) data.rows.get(0).values.get(sumCashDraw);
            if (sumCash == null) sumCash = toPay - sumCard;
        } else
            sumCash = toPay - sumCard;
        //result += sumCash / 100 + "\n";
        result.sumCash = sumCash;

        result.cashierName = (String) data.rows.get(0).values.get(cashierNameDraw);
        result.clientName = (String) data.rows.get(0).values.get(clientNameDraw);
        result.clientSum = (Double) data.rows.get(0).values.get(clientSumDraw);

        return result;
    }

    public ClientAction getPrintOrderAction(FormInstance formInstance,
                                            Set<GroupObjectInstance> classGroups,
                                            PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                            PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                            PropertyDrawEntity<?> toPayProp, PropertyDrawEntity<?> barcodeProp) {

        List<ClientAction> actions = new ArrayList<ClientAction>();
        int comPort = getCashRegComPort(formInstance);
        if (comPort > 0) {
            actions.add(new NonFiscalPrintAction(createOrderTxt(formInstance,
                    classGroups, priceProp, quantityProp,
                    nameProp, sumProp, toPayProp, barcodeProp), comPort));
        }
        return new ListClientAction(actions);
    }


    private String createOrderTxt(FormInstance formInstance, Set<GroupObjectInstance> classGroups,
                                  PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                  PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                  PropertyDrawEntity<?> toPayProp, PropertyDrawEntity<?> barcodeProp) {

        String result = "";

        FormData data;

        Set<PropertyDrawInstance> formProperties = new HashSet<PropertyDrawInstance>();
        PropertyDrawInstance quantityDraw = formInstance.instanceFactory.getInstance(quantityProp);
        PropertyDrawInstance priceDraw = formInstance.instanceFactory.getInstance(priceProp);
        PropertyDrawInstance nameDraw = formInstance.instanceFactory.getInstance(nameProp);
        PropertyDrawInstance barcodeDraw = formInstance.instanceFactory.getInstance(barcodeProp);
        PropertyDrawInstance sumDraw = formInstance.instanceFactory.getInstance(sumProp);
        PropertyDrawInstance toPayDraw = formInstance.instanceFactory.getInstance(toPayProp);
        formProperties.addAll(BaseUtils.toSet(quantityDraw, priceDraw, nameDraw, sumDraw, toPayDraw, barcodeDraw));

        quantityDraw.toDraw.addTempFilter(new NotNullFilterInstance(quantityDraw.propertyObject));

        try {
            data = formInstance.getFormData(formProperties, classGroups);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            quantityDraw.toDraw.clearTempFilters();
        }

        int totalDiscount = 0;

        final int lengthCheck = 28;
        for (FormRow row : data.rows) {

            Double quantity = (Double) row.values.get(quantityDraw);

            if (quantity != null) {

                Double price = BaseUtils.nvl((Double) row.values.get(priceDraw), 0.0);
                String barcode = (BaseUtils.nvl((String) row.values.get(barcodeDraw), "")).trim();
                String artName = (BaseUtils.nvl((String) row.values.get(nameDraw), "")).trim();
                //artName = artName.replace(',', '.');
                //artName = artName.replace('"', ' ');
                //artName = artName.replace('\'', ' ');
                Double sumPos = (Double) row.values.get(sumDraw);
                Double sumFull = price * quantity;

                int discount = ((Double) (sumFull - sumPos)).intValue();
                totalDiscount += discount;
                String priceString = quantity.intValue() + "x" + price.intValue() + (sumFull.equals(sumPos) ? "" : " ск. " + discount) + '=' + sumPos.intValue();
                result += BaseUtils.padr(barcode, lengthCheck) + '\n' + BaseUtils.padr(artName, lengthCheck)
                        + '\n' + BaseUtils.padl(priceString, lengthCheck) + '\n';
            }
        }

        String discountResult = "ОБЩ. СКИДКА: " + totalDiscount;

        Double toPay = data.rows.size() > 0 ? BaseUtils.nvl((Double) data.rows.get(0).values.get(toPayDraw), 0.0) : 0.0;
        String sumResult = "ИТОГО: " + toPay.intValue();
        result += '\n' + BaseUtils.padl(discountResult, lengthCheck) + '\n' + BaseUtils.padl(sumResult, lengthCheck) + '\n';

        return result;
    }

    public String checkCashRegApplyActions(Object result) {
        /*
        List<Object> listActions = (List<Object>) result;

        if (!noBillTxt) {
            ImportFileClientActionResult keyImpFileResult = ((ImportFileClientActionResult) listActions.get(listActions.size() - 3));
            ImportFileClientActionResult keyExImpFileResult = ((ImportFileClientActionResult) listActions.get(listActions.size() - 2));

            if (keyImpFileResult.fileExists && !keyExImpFileResult.fileExists) {
                return "Произошла ошибка при записи в ФР : программа взаимодействия с регистратором не загружена.\n" +
                        "Для ее загрузки нужно запустить на рабочем столе ярлык 'Гепард'.";
            }

            ImportFileClientActionResult errorImpFileResult = ((ImportFileClientActionResult) listActions.get(listActions.size() - 1));

            if (errorImpFileResult.fileExists) {
                return (errorImpFileResult.fileContent.isEmpty()) ? "Произошла ошибка нижнего уровня ФР" : ("Ошибка при записи на фискальный регистратор :" + errorImpFileResult.fileContent);
            }
        }
        */
        return null;
    }

    public void addCashRegProperties() {

//       пока не поддерживается
//        BL.addProp(BL.cashRegOperGroup, new SimpleCashRegActionProperty(BL.genSID(), "Аннулировать чек", "/A")); 
        BL.addProp(BL.cashRegOperGroup, new SimpleCashRegActionProperty(BL.genSID(), "Продолжить печать", "/R"));
        BL.addProp(BL.cashRegOperGroup, new MessageActionProperty(BL.genSID(), "Запрос наличных", MessageAction.COUNTER));
        BL.addProp(BL.cashRegOperGroup, new ReportActionProperty(BL.genSID(), "Открыть денеж. ящик", ReportAction.MONEY_BOX));
        BL.addProp(BL.cashRegAdminGroup, new MessageActionProperty(BL.genSID(), "Номера посл. чека", MessageAction.LAST_DOC_NUM));
        BL.addProp(BL.cashRegAdminGroup, new IntegerCashRegActionProperty(BL.genSID(), "Внесение денег", MoneyOperationAction.CASH_IN));
        BL.addProp(BL.cashRegAdminGroup, new IntegerCashRegActionProperty(BL.genSID(), "Изъятие денег", MoneyOperationAction.CASH_OUT));
        BL.addProp(BL.cashRegAdminGroup, new ReportActionProperty(BL.genSID(), "X-отчет (сменный отчет без гашения)", ReportAction.XREPORT));
        BL.addProp(BL.cashRegAdminGroup, new ReportActionProperty(BL.genSID(), "Z-отчет (сменный отчет с гашением)", ReportAction.ZREPORT));
        BL.addProp(BL.cashRegAdminGroup, new MessageActionProperty(BL.genSID(), "Запрос серийного номера регистратора", MessageAction.SERIAL_NUM));
    }

    private class ReportActionProperty extends ActionProperty {
        int type;

        public ReportActionProperty(String sID, String caption, int type) {
            super(sID, caption, new ValueClass[]{});
            this.type = type;
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            int comPort = getCashRegComPort(executeForm.form);
            if (comPort == 0) {
                return;
            }
            actions.add(new ReportAction(type, comPort));
        }
    }

    private class MessageActionProperty extends ActionProperty {
        int type;

        public MessageActionProperty(String sID, String caption, int type) {
            super(sID, caption, new ValueClass[]{});
            this.type = type;
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            int comPort = getCashRegComPort(executeForm.form);
            if (comPort == 0) {
                return;
            }
            actions.add(new MessageAction(type, comPort));
        }
    }

    private class SimpleCashRegActionProperty extends ActionProperty {

        String command, outputFile;
        int multiplier;
        String mask;

        private SimpleCashRegActionProperty(String sID, String caption, String command) {
            this(sID, caption, command, null);
        }

        private SimpleCashRegActionProperty(String sID, String caption, String command, String outputFile) {
            this(sID, caption, command, outputFile, 0);
        }

        private SimpleCashRegActionProperty(String sID, String caption, String command, String outputFile, int multiplier) {
            this(sID, caption, command, outputFile, multiplier, null);
        }

        private SimpleCashRegActionProperty(String sID, String caption, String command, String outputFile, int multiplier, String mask) {
            super(sID, caption, new ValueClass[]{});
            this.command = command;
            this.outputFile = outputFile;
            this.multiplier = multiplier;
            this.mask = mask;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {

            actions.add(new ExportFileClientAction("c:\\bill\\key.txt", false, command, CASHREGISTER_CHARSETNAME));
            actions.add(new SleepClientAction(CASHREGISTER_DELAY));
            actions.add(new MessageFileClientAction("c:\\bill\\error.txt", CASHREGISTER_CHARSETNAME, false, true, caption));

            if (outputFile != null) {
                actions.add(new MessageFileClientAction("c:\\bill\\" + outputFile, CASHREGISTER_CHARSETNAME, true, true, caption, multiplier, mask));
            }
        }
    }

    private class IntegerCashRegActionProperty extends ActionProperty {

        int type;

        private IntegerCashRegActionProperty(String sID, String caption, int command) {
            super(sID, caption, new ValueClass[]{});
            this.type = command;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            if (value.getValue() != null && value.getValue() instanceof Double) {
                int comPort = getCashRegComPort(executeForm.form);
                if (comPort == 0) {
                    return;
                }
                actions.add(new MoneyOperationAction(type, comPort, (Double) value.getValue()));
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
