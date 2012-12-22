package tmc.integration.exp;

import platform.base.BaseUtils;
import platform.base.col.SetFact;
import platform.interop.action.ClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.NotNullFilterInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.CustomReadValueActionProperty;
import platform.server.logics.property.actions.UserActionProperty;
import tmc.VEDLogicsModule;
import tmc.integration.exp.FiscalRegister.*;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;

public class CashRegController {

    private static String CASHREGISTER_CHARSETNAME = "Cp866";
    private static int CASHREGISTER_DELAY = 2000;

    VEDLogicsModule LM;

    public CashRegController(VEDLogicsModule LM) {
        this.LM = LM;
    }

    private boolean noBillTxt = false;
    private int cashRegComPort = 0;

    public int getCashRegComPort(FormInstance formInstance) {
        try {
            Integer result = (Integer) LM.cashRegComPort.read(formInstance, formInstance.instanceFactory.computer.getDataObject());
            if (result == null)
                return 0;
            else
                return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ClientAction getCashRegApplyActions(FormInstance formInstance, int payType,
                                                     Set<GroupObjectInstance> classGroups,
                                                     PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                                     PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                                     PropertyDrawEntity<?> toPayProp, PropertyDrawEntity<?> barcodeProp,
                                                     PropertyDrawEntity<?> sumCardProp, PropertyDrawEntity<?> sumCashProp,
                                                     PropertyDrawEntity<?> orderArticleSaleDiscount, PropertyDrawEntity<?> orderArticleSaleDiscountSum,
                                                     PropertyDrawEntity<?> cashierProp, PropertyDrawEntity<?> clientNameProp,
                                                     PropertyDrawEntity<?> clientSumProp, PropertyDrawEntity<?> discountProp, Set<GroupObjectInstance> obligationGrObj,
                                                     PropertyDrawEntity<?> obligationName, PropertyDrawEntity<?> obligationSum, PropertyDrawEntity<?> obligationBarcode) {

        cashRegComPort = getCashRegComPort(formInstance);
        if (cashRegComPort > 0) {
            return new CashRegPrintReceiptAction(payType, cashRegComPort, createReceipt(formInstance, payType,
                    classGroups, priceProp, quantityProp, nameProp,
                    sumProp, toPayProp, barcodeProp, sumCardProp, sumCashProp,
                    orderArticleSaleDiscount, orderArticleSaleDiscountSum, cashierProp, clientNameProp,
                    clientSumProp, discountProp, obligationGrObj, obligationName, obligationSum, obligationBarcode));

        }
        return null;
    }


    public ReceiptInstance createReceipt(FormInstance formInstance, int payType,
                                         Set<GroupObjectInstance> classGroups,
                                         PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                         PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                         PropertyDrawEntity<?> toPayProp, PropertyDrawEntity<?> barcodeProp,
                                         PropertyDrawEntity<?> sumCardProp, PropertyDrawEntity<?> sumCashProp,
                                         PropertyDrawEntity<?> orderArticleSaleDiscountProp,
                                         PropertyDrawEntity<?> orderArticleSaleDiscountSumProp, PropertyDrawEntity<?> cashierProp,
                                         PropertyDrawEntity<?> clientNameProp, PropertyDrawEntity<?> clientSumProp, PropertyDrawEntity<?> discountProp, Set<GroupObjectInstance> obligationGr,
                                         PropertyDrawEntity<?> obligationName, PropertyDrawEntity<?> obligationSum, PropertyDrawEntity<?> obligationBarcode) {

        ReceiptInstance result = new ReceiptInstance(payType);
        FormData data;
        FormData obligationData = null;

        Set<PropertyDrawInstance> formProperties = new HashSet<PropertyDrawInstance>();
        Set<PropertyDrawInstance> obligationProperties = new HashSet<PropertyDrawInstance>();
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

        PropertyDrawInstance clientDiscountDraw = null;
        if (discountProp != null) {
            clientDiscountDraw = formInstance.instanceFactory.getInstance(discountProp);
            formProperties.add(clientDiscountDraw);
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

        PropertyDrawInstance obligationNameDraw = null;
        if (obligationName != null) {
            obligationNameDraw = formInstance.instanceFactory.getInstance(obligationName);
            obligationProperties.add(obligationNameDraw);
        }

        PropertyDrawInstance obligationSumDraw = null;
        if (obligationSum != null) {
            obligationSumDraw = formInstance.instanceFactory.getInstance(obligationSum);
            obligationProperties.add(obligationSumDraw);
        }

        PropertyDrawInstance obligationBarcodeDraw = null;
        if (obligationBarcode != null) {
            obligationBarcodeDraw = formInstance.instanceFactory.getInstance(obligationBarcode);
            obligationProperties.add(obligationBarcodeDraw);
        }

        quantityDraw.toDraw.addTempFilter(new NotNullFilterInstance((CalcPropertyObjectInstance) quantityDraw.propertyObject));

        try {
            data = formInstance.getFormData(formProperties, classGroups);
            if (obligationGr != null) {
                obligationData = formInstance.getFormData(obligationProperties, obligationGr);
            }
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
                Number articleDiscSum = (Number) row.values.get(orderArticleDiscountSumDraw);

                result.addReceipt(new ReceiptItem(price, quantity, barcodeName, artName, sumPos,
                        articleDisc == null ? 0.0 : articleDisc, articleDiscSum == null ? 0 : articleDiscSum));

                sumDoc += price * quantity;
            }
        }
        result.sumTotal = sumDoc;

        if (obligationData != null) {
            for (FormRow row : obligationData.rows) {
                String nameObj = (String) row.values.get(obligationNameDraw);
                String barcodeObj = (String) row.values.get(obligationBarcodeDraw);
                Double sumObj = (Double) row.values.get(obligationSumDraw);
                result.addObligation(new ObligationItem(nameObj, barcodeObj, sumObj));
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
        result.clientDiscount = (Number) data.rows.get(0).values.get(clientDiscountDraw);

        return result;
    }

    public ClientAction getPrintOrderAction(FormInstance formInstance,
                                            Set<GroupObjectInstance> classGroups,
                                            PropertyDrawEntity<?> priceProp, PropertyDrawEntity<?> quantityProp,
                                            PropertyDrawEntity<?> nameProp, PropertyDrawEntity<?> sumProp,
                                            PropertyDrawEntity<?> toPayProp, PropertyDrawEntity<?> barcodeProp) {

        int comPort = getCashRegComPort(formInstance);
        if (comPort > 0) {
            return new NonFiscalPrintAction(createOrderTxt(formInstance,
                    classGroups, priceProp, quantityProp,
                    nameProp, sumProp, toPayProp, barcodeProp), comPort);
        }
        return null;
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

        quantityDraw.toDraw.addTempFilter(new NotNullFilterInstance((CalcPropertyObjectInstance) quantityDraw.propertyObject));

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

            Object quantity = row.values.get(quantityDraw);

            if (quantity != null) {
                
                Double doubleQuantity = (quantity instanceof Double ? (Double)quantity : 1.0);

                Double price = BaseUtils.nvl((Double) row.values.get(priceDraw), 0.0);
                String barcode = (BaseUtils.nvl((String) row.values.get(barcodeDraw), "")).trim();
                String artName = (BaseUtils.nvl((String) row.values.get(nameDraw), "")).trim();
                //artName = artName.replace(',', '.');
                //artName = artName.replace('"', ' ');
                //artName = artName.replace('\'', ' ');
                Double sumPos = (Double) row.values.get(sumDraw);
                Double sumFull = price * doubleQuantity;

                int discount = ((Double) (sumFull - sumPos)).intValue();
                totalDiscount += discount;
                String priceString = doubleQuantity.intValue() + "x" + price.intValue() + (sumFull.equals(sumPos) ? "" : " ск. " + discount) + '=' + sumPos.intValue();
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

    public void addCashRegProperties() {
//       пока не поддерживается
//        LM.addProp(LM.cashRegOperGroup, new SimpleCashRegActionProperty(LM.genSID(), "Аннулировать чек", "/A"));
        LM.addProp(LM.cashRegOperGroup, new SimpleCashRegActionProperty(LM.baseLM.genSID(), "Продолжить печать", "/R"));
        LM.addProp(LM.cashRegOperGroup, new MessageActionProperty(LM.baseLM.genSID(), "Запрос наличных", MessageAction.COUNTER));
        LM.addProp(LM.cashRegOperGroup, new ReportActionProperty(LM.baseLM.genSID(), "Открыть денеж. ящик", ReportAction.MONEY_BOX));
        LM.addProp(LM.cashRegAdminGroup, new MessageActionProperty(LM.baseLM.genSID(), "Номера посл. чека", MessageAction.LAST_DOC_NUM));
        LM.addProp(LM.cashRegAdminGroup, new IntegerCashRegActionProperty(LM.baseLM.genSID(), "Внесение денег", MoneyOperationAction.CASH_IN));
        LM.addProp(LM.cashRegAdminGroup, new IntegerCashRegActionProperty(LM.baseLM.genSID(), "Изъятие денег", MoneyOperationAction.CASH_OUT));
        LM.addProp(LM.cashRegAdminGroup, new ReportActionProperty(LM.baseLM.genSID(), "X-отчет (сменный отчет без гашения)", ReportAction.XREPORT));
        ReportActionProperty zProp = new ReportActionProperty(LM.baseLM.genSID(), "Z-отчет (сменный отчет с гашением)", ReportAction.ZREPORT);
        zProp.askConfirm = true;
        LM.addProp(LM.cashRegAdminGroup, zProp);
        LM.addProp(LM.cashRegAdminGroup, new MessageActionProperty(LM.baseLM.genSID(), "Запрос серийного номера регистратора", MessageAction.SERIAL_NUM));
    }

    private class ReportActionProperty extends UserActionProperty {
        int type;

        public ReportActionProperty(String sID, String caption, int type) {
            super(sID, caption, new ValueClass[]{});
            this.type = type;
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            int comPort = getCashRegComPort(context.getFormInstance());
            if (comPort == 0) {
                return;
            }
            context.delayUserInterfaction(new ReportAction(type, comPort));
        }
    }

    private class MessageActionProperty extends UserActionProperty {
        int type;

        public MessageActionProperty(String sID, String caption, int type) {
            super(sID, caption, new ValueClass[]{});
            this.type = type;
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            int comPort = getCashRegComPort(context.getFormInstance());
            if (comPort == 0) {
                return;
            }
            context.delayUserInterfaction(new MessageAction(type, comPort));
        }
    }

    private class SimpleCashRegActionProperty extends UserActionProperty {

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

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            //пока отключен, чтобы не вылетал exception

            /*
            actions.add(new ExportFileClientAction("c:\\bill\\key.txt", command, CASHREGISTER_CHARSETNAME));
            actions.add(new SleepClientAction(CASHREGISTER_DELAY));
            actions.add(new MessageFileClientAction("c:\\bill\\error.txt", CASHREGISTER_CHARSETNAME, false, true, caption));

            if (outputFile != null) {
                actions.add(new MessageFileClientAction("c:\\bill\\" + outputFile, CASHREGISTER_CHARSETNAME, true, true, caption, multiplier, mask));
            }
            */
        }
    }

    private class IntegerCashRegActionProperty extends CustomReadValueActionProperty {

        int type;

        private IntegerCashRegActionProperty(String sID, String caption, int command) {
            super(sID, caption, new ValueClass[]{});
            this.type = command;
        }

        protected DataClass getReadType() {
            return DoubleClass.instance;
        }

        protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object countValue) throws SQLException {
            if (countValue instanceof Double) {
                int comPort = getCashRegComPort(context.getFormInstance());
                if (comPort == 0) {
                    return;
                }
                context.delayUserInterfaction(new MoneyOperationAction(type, comPort, (Double) countValue));
            }
        }
    }

    public FormEntity createCashRegManagementFormEntity(NavigatorElement parent, String sID) {
        return new CashRegManagementFormEntity(parent, sID);
    }

    private class CashRegManagementFormEntity extends FormEntity {

        private CashRegManagementFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Операции с ФР");
            addPropertyDraw(LM.cashRegGroup, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setFont(VEDLogicsModule.FONT_HUGE_BOLD);
            design.setPanelLabelAbove(LM.baseLM.baseGroup, true);

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }
    }

}
