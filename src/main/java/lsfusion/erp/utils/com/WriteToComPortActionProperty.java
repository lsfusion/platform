package lsfusion.erp.utils.com;

import com.google.common.base.Throwables;
import jssc.SerialPort;
import jssc.SerialPortException;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Iterator;

public class WriteToComPortActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface baudRateInterface;
    private final ClassPropertyInterface comPortInterface;

    public WriteToComPortActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        textInterface = i.next();
        charsetInterface = i.next();
        baudRateInterface = i.next();
        comPortInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String text = (String) context.getDataKeyValue(textInterface).object;
            String charset = (String) context.getDataKeyValue(charsetInterface).object;
            Integer baudRate = (Integer) context.getDataKeyValue(baudRateInterface).object;
            Integer comPort = (Integer) context.getDataKeyValue(comPortInterface).object;

            if (text != null && charset != null && baudRate != null && comPort != null) {

                SerialPort serialPort = new SerialPort("COM" + comPort);
                serialPort.openPort();
                serialPort.setParams(baudRate, 8, 1, 0);
                serialPort.writeBytes(text.getBytes(Charset.forName(charset)));
                serialPort.closePort();

            } else {
                context.delayUserInteraction(new MessageClientAction("Не все параметры заданы", "Ошибка"));
            }
        } catch (SerialPortException e) {
            throw Throwables.propagate(e);
        }
    }
}
