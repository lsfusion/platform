package fdk.retail;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ExportReceiptsActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface zReportInterface;

    public ExportReceiptsActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("ZReport")});

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

            DataObject zReportObject = context.getDataKeyValue(zReportInterface);
            String numberZReport = (String) LM.findLCPByCompoundName("numberZReport").read(session, zReportObject);

            KeyExpr receiptExpr = new KeyExpr("receipt");
            ImRevMap<Object, KeyExpr> receiptKeys = MapFact.singletonRev((Object) "receipt", receiptExpr);

            String[] receiptProperties = new String[]{"dateTimeReceipt", "discountSumReceipt",
                    "numberDiscountCardReceipt"};
            QueryBuilder<Object, Object> receiptQuery = new QueryBuilder<Object, Object>(receiptKeys);
            for (String rProperty : receiptProperties) {
                receiptQuery.addProperty(rProperty, getLCP(rProperty).getExpr(context.getModifier(), receiptExpr));
            }
            receiptQuery.and(getLCP("zReportReceipt").getExpr(context.getModifier(), receiptQuery.getMapExprs().get("receipt")).compare(zReportObject.getExpr(), Compare.EQUALS));
            receiptQuery.and(getLCP("exportReceipt").getExpr(context.getModifier(), receiptQuery.getMapExprs().get("receipt")).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptResult = receiptQuery.execute(session.sql);

            for (int i=0,size=receiptResult.size();i<size;i++) {
                DataObject receiptObject = new DataObject(receiptResult.getKey(i).get("receipt"), (ConcreteClass) LM.findClassByCompoundName("receipt"));
                LM.findLCPByCompoundName("exportReceipt").change((Object)null, context.getSession(), receiptObject);

                Element receipt = doc.createElement("receipt");
                rootElement.appendChild(receipt);

                KeyExpr receiptDetailExpr = new KeyExpr("receiptDetail");
                ImRevMap<Object, KeyExpr> receiptDetailKeys = MapFact.singletonRev((Object)"receiptDetail", receiptDetailExpr);

                String[] receiptDetailProperties = new String[]{"quantityReceiptSaleDetail", "quantityReceiptReturnDetail",
                        "priceReceiptDetail", "idBarcodeReceiptDetail", "sumReceiptDetail", "discountSumReceiptDetail"};
                QueryBuilder<Object, Object> receiptDetailQuery = new QueryBuilder<Object, Object>(receiptDetailKeys);
                for (String rdProperty : receiptDetailProperties) {
                    receiptDetailQuery.addProperty(rdProperty, getLCP(rdProperty).getExpr(context.getModifier(), receiptDetailExpr));
                }
                receiptDetailQuery.and(getLCP("receiptReceiptDetail").getExpr(context.getModifier(), receiptDetailQuery.getMapExprs().get("receiptDetail")).compare(receiptObject.getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptDetailResult = receiptDetailQuery.execute(context.getSession().sql);

                int numberReceiptDetail = 1;
                for (ImMap<Object, Object> receiptDetailValues : receiptDetailResult.valueIt()) {
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
                ImRevMap<Object, KeyExpr> paymentKeys = MapFact.singletonRev((Object)"payment", paymentExpr);

                QueryBuilder<Object, Object> paymentQuery = new QueryBuilder<Object, Object>(paymentKeys);
                paymentQuery.addProperty("sumPayment", getLCP("sumPayment").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.addProperty("paymentMeansPayment", getLCP("paymentMeansPayment").getExpr(context.getModifier(), paymentExpr));
                paymentQuery.addProperty("sidPaymentTypePayment", getLCP("sidPaymentTypePayment").getExpr(context.getModifier(), paymentExpr));

                paymentQuery.and(getLCP("receiptPayment").getExpr(context.getModifier(), paymentQuery.getMapExprs().get("payment")).compare(receiptObject.getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> paymentResult = paymentQuery.execute(context.getSession().sql);
                for (ImMap<Object, Object> paymentValues : paymentResult.valueIt()) {
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
                    Object value = receiptResult.getValue(i).get(field);
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
