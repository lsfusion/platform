package platform.fdk.actions;

import jxl.write.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ConcreteClass;
import platform.server.classes.StaticCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

public class ExportReceiptsActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface zReportInterface;

    public ExportReceiptsActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("zReport")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        zReportInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        try {
            DataSession session = context.getSession();
            Map<String, byte[]> files = new HashMap<String, byte[]>();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("zReport");
            doc.appendChild(rootElement);

            DataObject zReportObject = context.getKeyValue(zReportInterface);
            String numberZReport = (String) LM.findLCPByCompoundName("numberZReport").read(session, zReportObject);

            KeyExpr receiptExpr = new KeyExpr("receipt");
            Map<Object, KeyExpr> receiptKeys = new HashMap<Object, KeyExpr>();
            receiptKeys.put("receipt", receiptExpr);

            String[] receiptProperties = new String[]{"dateTimeReceipt", "discountSumReceipt",
                    "numberDiscountCardReceipt"};
            Query<Object, Object> receiptQuery = new Query<Object, Object>(receiptKeys);
            for (String rProperty : receiptProperties) {
                receiptQuery.properties.put(rProperty, getLCP(rProperty).getExpr(context.getModifier(), receiptExpr));
            }
            receiptQuery.and(getLCP("zReportReceipt").getExpr(context.getModifier(), receiptQuery.mapKeys.get("receipt")).compare(zReportObject.getExpr(), Compare.EQUALS));
            receiptQuery.and(getLCP("exportReceipt").getExpr(context.getModifier(), receiptQuery.mapKeys.get("receipt")).getWhere());

            OrderedMap<Map<Object, Object>, Map<Object, Object>> receiptResult = receiptQuery.execute(session.sql);

            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> receiptRows : receiptResult.entrySet()) {
                DataObject receiptObject = new DataObject(receiptRows.getKey().get("receipt"), (ConcreteClass) LM.findClassByCompoundName("receipt"));
                LM.findLCPByCompoundName("exportReceipt").change(null, context.getSession(), receiptObject);

                Element receipt = doc.createElement("receipt");
                rootElement.appendChild(receipt);

                KeyExpr receiptDetailExpr = new KeyExpr("receiptDetail");
                Map<Object, KeyExpr> receiptDetailKeys = new HashMap<Object, KeyExpr>();
                receiptDetailKeys.put("receiptDetail", receiptDetailExpr);

                String[] receiptDetailProperties = new String[]{"quantityReceiptSaleDetail", "quantityReceiptReturnDetail",
                        "priceReceiptDetail", "idBarcodeReceiptDetail", "sumReceiptDetail", "discountSumReceiptDetail"};
                Query<Object, Object> receiptDetailQuery = new Query<Object, Object>(receiptDetailKeys);
                for (String rdProperty : receiptDetailProperties) {
                    receiptDetailQuery.properties.put(rdProperty, getLCP(rdProperty).getExpr(context.getModifier(), receiptDetailExpr));
                }
                receiptDetailQuery.and(getLCP("receiptReceiptDetail").getExpr(context.getModifier(), receiptDetailQuery.mapKeys.get("receiptDetail")).compare(receiptObject.getExpr(), Compare.EQUALS));

                OrderedMap<Map<Object, Object>, Map<Object, Object>> receiptDetailResult = receiptDetailQuery.execute(context.getSession().sql);

                int numberReceiptDetail = 1;
                for (Map<Object, Object> receiptDetailValues : receiptDetailResult.values()) {
                    Element receiptDetail = doc.createElement("receiptDetail");
                    rootElement.appendChild(receiptDetail);

                    String[] fields = new String[]{"priceReceiptDetail", "quantityReceiptSaleDetail",
                            "quantityReceiptReturnDetail", "idBarcodeReceiptDetail", "sumReceiptDetail",
                            "discountSumReceiptDetail", "numberReceiptDetail"};

                    for (String field : fields) {
                        Object value = receiptDetailValues.get(field);
                        if (field.equals("numberReceiptDetail")) {
                            value = numberReceiptDetail;
                            numberReceiptDetail++;
                        }
                        if (value != null) {
                            Element element = doc.createElement(field);
                            element.appendChild(doc.createTextNode(String.valueOf(value)));
                            receiptDetail.appendChild(element);
                        }
                    }
                    receipt.appendChild(receiptDetail);
                }
                KeyExpr paymentExpr = new KeyExpr("payment");
                Map<Object, KeyExpr> paymentKeys = new HashMap<Object, KeyExpr>();
                paymentKeys.put("payment", paymentExpr);

                Query<Object, Object> paymentQuery = new Query<Object, Object>(paymentKeys);
                paymentQuery.properties.put("sumPayment", getLCP("sumPayment").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.properties.put("paymentMeansPayment", getLCP("paymentMeansPayment").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.properties.put("sidPaymentTypePayment", getLCP("sidPaymentTypePayment").getExpr(context.getModifier(), paymentExpr));

                paymentQuery.and(getLCP("receiptPayment").getExpr(context.getModifier(), paymentQuery.mapKeys.get("payment")).compare(receiptObject.getExpr(), Compare.EQUALS));

                OrderedMap<Map<Object, Object>, Map<Object, Object>> paymentResult = paymentQuery.execute(context.getSession().sql);
                for (Map<Object, Object> paymentValues : paymentResult.values()) {
                    Element payment = doc.createElement("payment");
                    rootElement.appendChild(payment);

                    String[] fields = new String[]{"sumPayment", "paymentMeansPayment", "sidPaymentTypePayment"};
                    for (String field : fields) {
                        Object value = paymentValues.get(field);
                        if (value != null) {
                            Element element = doc.createElement(field);
                            element.appendChild(doc.createTextNode(String.valueOf(value)));
                            payment.appendChild(element);
                        }
                    }
                    receipt.appendChild(payment);
                }

                String[] fields = new String[]{"dateTimeReceipt", "discountSumReceipt", "numberDiscountCardReceipt"};
                for (String field : fields) {
                    Object value = receiptRows.getValue().get(field);
                    if (value != null) {
                        Element element = doc.createElement(field);
                        if (value instanceof Timestamp)
                            element.appendChild(doc.createTextNode(String.valueOf(((Timestamp) value).getTime())));
                        else
                            element.appendChild(doc.createTextNode(String.valueOf(value)));
                        receipt.appendChild(element);
                    }
                }
                rootElement.appendChild(receipt);
            }

            // write the content into xml file
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            String xmlString = sw.toString();
            File file = File.createTempFile("export", ".xml");
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(file), "UTF8"));
            writer.println(xmlString);
            writer.close();
            files.put(numberZReport.trim()+".xml", IOUtils.getFileBytes(file));
            context.delayUserInterfaction(new ExportFileClientAction(files));

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (TransformerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


}
