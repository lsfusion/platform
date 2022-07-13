package lsfusion.server.logics.classes.data;

import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.file.*;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.data.link.*;
import lsfusion.server.logics.classes.data.time.*;
import lsfusion.server.logics.property.data.SessionDataProperty;

import java.sql.SQLException;

public class AnyValuePropertyHolder {
    private final LP objectProperty;
    private final LP stringProperty;
    private final LP bpStringProperty;
    private final LP textProperty;
    private final LP richTextProperty;
    private final LP htmlTextProperty;
    private final LP intProperty;
    private final LP longProperty;
    private final LP doubleProperty;
    private final LP numericProperty;
    private final LP yearProperty;
    private final LP dateTimeProperty;
    private final LP zDateTimeProperty;
    private final LP intervalDateProperty;
    private final LP intervalDateTimeProperty;
    private final LP intervalZDateTimeProperty;
    private final LP intervalTimeProperty;
    private final LP logicalProperty;
    private final LP tLogicalProperty;
    private final LP dateProperty;
    private final LP timeProperty;
    private final LP colorProperty;
    private final LP jsonProperty;
    private final LP wordFileProperty;
    private final LP imageFileProperty;
    private final LP pdfFileProperty;
    private final LP dbfFileProperty;
    private final LP rawFileProperty;
    private final LP customFileProperty;
    private final LP excelFileProperty;
    private final LP textFileProperty;
    private final LP csvFileProperty;
    private final LP htmlFileProperty;
    private final LP jsonFileProperty;
    private final LP xmlFileProperty;
    private final LP tableFileProperty;
    private final LP namedFileProperty;
    private final LP wordLinkProperty;
    private final LP imageLinkProperty;
    private final LP pdfLinkProperty;
    private final LP dbfLinkProperty;
    private final LP rawLinkProperty;
    private final LP customLinkProperty;
    private final LP excelLinkProperty;
    private final LP textLinkProperty;
    private final LP csvLinkProperty;
    private final LP htmlLinkProperty;
    private final LP jsonLinkProperty;
    private final LP xmlLinkProperty;
    private final LP tableLinkProperty;

    public AnyValuePropertyHolder(LP<?> objectProperty, LP<?> stringProperty, LP<?> bpStringProperty, LP<?> textProperty, LP<?> richTextProperty, LP<?> htmlTextProperty,
                                  LP<?> intProperty, LP<?> longProperty, LP<?> doubleProperty, LP<?> numericProperty, LP<?> yearProperty, LP<?> dateTimeProperty,
                                  LP<?> zDateTimeProperty, LP<?> intervalDateProperty, LP<?> intervalDateTimeProperty, LP<?> intervalTimeProperty, LP<?> intervalZDateTimeProperty,
                                  LP<?> logicalProperty, LP<?> tLogicalProperty, LP<?> dateProperty, LP<?> timeProperty, LP<?> colorProperty, LP<?> jsonProperty,
                                  LP<?> wordFileProperty, LP<?> imageFileProperty, LP<?> pdfFileProperty, LP<?> dbfFileProperty,
                                  LP<?> rawFileProperty, LP<?> customFileProperty, LP<?> excelFileProperty, LP<?> textFileProperty, LP<?> csvFileProperty,
                                  LP<?> htmlFileProperty, LP<?> jsonFileProperty, LP<?> xmlFileProperty, LP<?> tableFileProperty, LP<?> namedFileProperty,
                                  LP<?> wordLinkProperty, LP<?> imageLinkProperty, LP<?> pdfLinkProperty, LP<?> dbfLinkProperty, LP<?> rawLinkProperty,
                                  LP<?> customLinkProperty, LP<?> excelLinkProperty, LP<?> textLinkProperty, LP<?> csvLinkProperty,
                                  LP<?> htmlLinkProperty, LP<?> jsonLinkProperty, LP<?> xmlLinkProperty, LP<?> tableLinkProperty) {
        assert objectProperty.property.getType() == ObjectType.instance
                && stringProperty.property.getType().getCompatible(StringClass.get(1))!=null
                && bpStringProperty.property.getType().getCompatible(StringClass.get(1))!=null
                && textProperty.property.getType().getCompatible(StringClass.get(1))!=null
                && richTextProperty.property.getType().getCompatible(RichTextClass.instance)!=null
                && htmlTextProperty.property.getType().getCompatible(HTMLTextClass.instance)!=null
                && intProperty.property.getType() == IntegerClass.instance
                && longProperty.property.getType() == LongClass.instance
                && doubleProperty.property.getType() == DoubleClass.instance
                && numericProperty.property.getType().getCompatible(NumericClass.get(0, 0)) != null
                && yearProperty.property.getType() == YearClass.instance
                && dateTimeProperty.property.getType().getCompatible(DateTimeClass.dateTime) != null
                && zDateTimeProperty.property.getType().getCompatible(ZDateTimeClass.zDateTime) != null
                && intervalDateProperty.property.getType().getCompatible(IntervalClass.getInstance("DATE")) != null
                && intervalDateTimeProperty.property.getType().getCompatible(IntervalClass.getInstance("DATETIME")) != null
                && intervalTimeProperty.property.getType().getCompatible(IntervalClass.getInstance("TIME")) != null
                && intervalZDateTimeProperty.property.getType().getCompatible(IntervalClass.getInstance("ZDATETIME")) != null
                && logicalProperty.property.getType() == LogicalClass.instance
                && tLogicalProperty.property.getType() == LogicalClass.threeStateInstance
                && dateProperty.property.getType() == DateClass.instance
                && timeProperty.property.getType().getCompatible(TimeClass.time) != null
                && colorProperty.property.getType() == ColorClass.instance
                && jsonProperty.property.getType() == JSONClass.instance
                && wordFileProperty.property.getType() == WordClass.get()
                && imageFileProperty.property.getType() == ImageClass.get()
                && pdfFileProperty.property.getType() == PDFClass.get()
                && dbfFileProperty.property.getType() == DBFClass.get()
                && rawFileProperty.property.getType() == CustomStaticFormatFileClass.get()
                && customFileProperty.property.getType() == DynamicFormatFileClass.get()
                && excelFileProperty.property.getType() == ExcelClass.get()
                && textFileProperty.property.getType() == TXTClass.get()
                && csvFileProperty.property.getType() == CSVClass.get()
                && htmlFileProperty.property.getType() == HTMLClass.get()
                && jsonFileProperty.property.getType() == JSONFileClass.get()
                && xmlFileProperty.property.getType() == XMLClass.get()
                && tableFileProperty.property.getType() == TableClass.get()
                && namedFileProperty.property.getType() == NamedFileClass.instance
                && wordLinkProperty.property.getType() == WordLinkClass.get(false)
                && imageLinkProperty.property.getType() == ImageLinkClass.get(false)
                && pdfLinkProperty.property.getType() == PDFLinkClass.get(false)
                && dbfLinkProperty.property.getType() == DBFLinkClass.get(false)
                && rawLinkProperty.property.getType() == CustomStaticFormatLinkClass.get()
                && customLinkProperty.property.getType() == DynamicFormatLinkClass.get(false)
                && excelLinkProperty.property.getType() == ExcelLinkClass.get(false)
                && textLinkProperty.property.getType() == TXTLinkClass.get(false)
                && csvLinkProperty.property.getType() == CSVLinkClass.get(false)
                && htmlLinkProperty.property.getType() == HTMLLinkClass.get(false)
                && jsonLinkProperty.property.getType() == JSONLinkClass.get(false)
                && xmlLinkProperty.property.getType() == XMLLinkClass.get(false)
                && tableLinkProperty.property.getType() == TableLinkClass.get(false)
                ;

        this.objectProperty = objectProperty;
        this.stringProperty = stringProperty;
        this.bpStringProperty = bpStringProperty;
        this.textProperty = textProperty;
        this.richTextProperty = richTextProperty;
        this.htmlTextProperty = htmlTextProperty;
        this.intProperty = intProperty;
        this.longProperty = longProperty;
        this.doubleProperty = doubleProperty;
        this.numericProperty = numericProperty;
        this.yearProperty = yearProperty;
        this.dateTimeProperty = dateTimeProperty;
        this.zDateTimeProperty = zDateTimeProperty;
        this.intervalDateProperty = intervalDateProperty;
        this.intervalDateTimeProperty = intervalDateTimeProperty;
        this.intervalTimeProperty = intervalTimeProperty;
        this.intervalZDateTimeProperty = intervalZDateTimeProperty;
        this.logicalProperty = logicalProperty;
        this.tLogicalProperty = tLogicalProperty;
        this.dateProperty = dateProperty;
        this.timeProperty = timeProperty;
        this.colorProperty = colorProperty;
        this.jsonProperty = jsonProperty;
        this.wordFileProperty = wordFileProperty;
        this.imageFileProperty = imageFileProperty;
        this.pdfFileProperty = pdfFileProperty;
        this.dbfFileProperty = dbfFileProperty;
        this.rawFileProperty = rawFileProperty;
        this.customFileProperty = customFileProperty;
        this.excelFileProperty = excelFileProperty;
        this.textFileProperty = textFileProperty;
        this.csvFileProperty = csvFileProperty;
        this.htmlFileProperty = htmlFileProperty;
        this.jsonFileProperty = jsonFileProperty;
        this.xmlFileProperty = xmlFileProperty;
        this.tableFileProperty = tableFileProperty;
        this.namedFileProperty = namedFileProperty;
        this.wordLinkProperty = wordLinkProperty;
        this.imageLinkProperty = imageLinkProperty;
        this.pdfLinkProperty = pdfLinkProperty;
        this.dbfLinkProperty = dbfLinkProperty;
        this.rawLinkProperty = rawLinkProperty;
        this.customLinkProperty = customLinkProperty;
        this.excelLinkProperty = excelLinkProperty;
        this.textLinkProperty = textLinkProperty;
        this.csvLinkProperty = csvLinkProperty;
        this.htmlLinkProperty = htmlLinkProperty;
        this.jsonLinkProperty = jsonLinkProperty;
        this.xmlLinkProperty = xmlLinkProperty;
        this.tableLinkProperty = tableLinkProperty;
    }

    public LP<?> getLP(Type valueType) {
        if (valueType instanceof ObjectType)
            return objectProperty;
        else if (valueType instanceof RichTextClass)
            return richTextProperty;
        else if (valueType instanceof HTMLTextClass)
            return htmlTextProperty;
        else if (valueType instanceof TextClass)
            return textProperty;
        else if (valueType instanceof StringClass) {
            return ((StringClass) valueType).blankPadded ? bpStringProperty : stringProperty;
        } else if (valueType instanceof IntegerClass) {
            if (valueType instanceof YearClass) {
                return yearProperty;
            }
            return intProperty;
        } else if (valueType instanceof LongClass) {
            return longProperty;
        } else if (valueType instanceof DoubleClass) {
            return doubleProperty;
        } else if (valueType instanceof NumericClass) {
            return numericProperty;
        } else if (valueType instanceof DateTimeClass) {
            return dateTimeProperty;
        } else if (valueType instanceof ZDateTimeClass) {
            return zDateTimeProperty;
        } else if (valueType instanceof DateIntervalClass) {
            return intervalDateProperty;
        } else if (valueType instanceof DateTimeIntervalClass) {
            return intervalDateTimeProperty;
        } else if (valueType instanceof TimeIntervalClass) {
            return intervalTimeProperty;
        } else if (valueType instanceof ZDateTimeIntervalClass) {
            return intervalZDateTimeProperty;
        } else if (valueType instanceof LogicalClass) {
            return ((LogicalClass) valueType).threeState ? tLogicalProperty : logicalProperty;
        } else if (valueType instanceof DateClass) {
            return dateProperty;
        } else if (valueType instanceof TimeClass) {
            return timeProperty;
        } else if (valueType instanceof ColorClass) {
            return colorProperty;
        } else if (valueType instanceof JSONClass) {
            return jsonProperty;
        } else if (valueType instanceof WordClass) {
            return wordFileProperty;
        } else if (valueType instanceof ImageClass) {
            return imageFileProperty;
        } else if (valueType instanceof PDFClass) {
            return pdfFileProperty;
        } else if (valueType instanceof DBFClass) {
            return dbfFileProperty;
        } else if (valueType instanceof CustomStaticFormatFileClass) {
            return rawFileProperty;
        }else if (valueType instanceof NamedFileClass) {
            return namedFileProperty;
        } else if (valueType instanceof DynamicFormatFileClass) {
            return customFileProperty;
        } else if (valueType instanceof ExcelClass) {
            return excelFileProperty;
        } else if (valueType instanceof TXTClass) {
            return textFileProperty;
        } else if (valueType instanceof CSVClass) {
            return csvFileProperty;
        } else if (valueType instanceof HTMLClass) {
            return htmlFileProperty;
        } else if (valueType instanceof JSONFileClass) {
            return jsonFileProperty;
        } else if (valueType instanceof XMLClass) {
            return xmlFileProperty;
        } else if (valueType instanceof TableClass) {
            return tableFileProperty;
        } else if (valueType instanceof StaticFormatFileClass) {
            return customFileProperty;
        } else if (valueType instanceof WordLinkClass) {
            return wordLinkProperty;
        } else if (valueType instanceof ImageLinkClass) {
            return imageLinkProperty;
        } else if (valueType instanceof PDFLinkClass) {
            return pdfLinkProperty;
        } else if (valueType instanceof DBFLinkClass) {
            return dbfLinkProperty;
        } else if (valueType instanceof CustomStaticFormatLinkClass) {
            return rawLinkProperty;
        } else if (valueType instanceof DynamicFormatLinkClass) {
            return customLinkProperty;
        } else if (valueType instanceof ExcelLinkClass) {
            return excelLinkProperty;
        } else if (valueType instanceof TXTLinkClass) {
            return textLinkProperty;
        } else if (valueType instanceof CSVLinkClass) {
            return csvLinkProperty;
        } else if (valueType instanceof HTMLLinkClass) {
            return htmlLinkProperty;
        } else if (valueType instanceof JSONLinkClass) {
            return jsonLinkProperty;
        } else if (valueType instanceof XMLLinkClass) {
            return xmlLinkProperty;
        } else if (valueType instanceof TableLinkClass) {
            return tableLinkProperty;
        } else if (valueType instanceof StaticFormatLinkClass) {
            return customLinkProperty;
        } else {
            throw new IllegalStateException(valueType + " is not supported by AnyValueProperty");
        }
    }
    
    @IdentityLazy
    public ImOrderSet<SessionDataProperty> getProps() {
        return SetFact.toOrderExclSet(
                // files
                customFileProperty, rawFileProperty, wordFileProperty, imageFileProperty, pdfFileProperty, dbfFileProperty, excelFileProperty,
                textFileProperty, csvFileProperty, htmlFileProperty, jsonFileProperty, xmlFileProperty, tableFileProperty, namedFileProperty,
                // strings
                textProperty, richTextProperty, htmlTextProperty, stringProperty, bpStringProperty,
                // numbers
                numericProperty, longProperty, intProperty, doubleProperty,
                // date / times
                dateTimeProperty, zDateTimeProperty, intervalDateProperty, intervalDateTimeProperty, intervalTimeProperty,
                intervalZDateTimeProperty, dateProperty, timeProperty, yearProperty,
                // links
                customLinkProperty, rawLinkProperty, wordLinkProperty, imageLinkProperty, pdfLinkProperty, dbfLinkProperty, excelLinkProperty,
                textLinkProperty, csvLinkProperty, htmlLinkProperty, jsonLinkProperty, xmlLinkProperty, tableLinkProperty,
                // others
                logicalProperty, tLogicalProperty, colorProperty, jsonProperty, objectProperty
        ).mapOrderSetValues(value -> (SessionDataProperty) value.property);
    }

    private static ObjectValue getFirstChangeProp(ImOrderSet<SessionDataProperty> props, Action<?> action, Result<SessionDataProperty> readedProperty) {
        ImOrderSet<SessionDataProperty> changedProps = SetFact.filterOrderFn(props, action.getChangeExtProps().keys());
        if(changedProps.isEmpty())
            changedProps = props;
        
        readedProperty.set(changedProps.get(0));
        return NullValue.instance;
    }
    
    public ObjectValue readFirstNotNull(ExecutionEnvironment env, Result<SessionDataProperty> readedProperty, Action<?> action) throws SQLException, SQLHandledException { // return, not drop first value
        DataSession session = env.getSession();

        ImOrderSet<SessionDataProperty> props = getProps();
        ImSet<SessionDataProperty> changedProps = SetFact.fromJavaSet(session.getSessionChanges(props.getSet()));

        if(changedProps.isEmpty()) // optimization
            return getFirstChangeProp(props, action, readedProperty);
        if(changedProps.size() == 1) { // optimization
            SessionDataProperty prop = changedProps.single();
            readedProperty.set(prop);
            return prop.readClasses(env);
        }

        for(SessionDataProperty prop : props.filterOrder(changedProps)) {
            ObjectValue changedValue = prop.readClasses(env);
            if (changedValue instanceof DataObject) {
                readedProperty.set(prop);
                return changedValue;
            }
        }
        return getFirstChangeProp(props, action, readedProperty);
    }
}
