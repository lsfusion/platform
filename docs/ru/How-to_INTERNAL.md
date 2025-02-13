---
title: 'How-to: INTERNAL'
---

## Пример 1

### Условие

Нужно реализовать действие, которое выдаст пользователю сообщение с именем и IP-адресом сервера.

### Решение

```lsf
ip = DATA LOCAL TEXT();
getIPJava INTERNAL 'GetIP';
showIPJava 'Показать имя компьютера (Java)' {
    getIPJava();
    MESSAGE ip();
}

FORM info 'Информация'
    PROPERTIES() showIPJava
;

NAVIGATOR {
    NEW info;
}
```

Для решения задачи необходимо создать действие при помощи [оператора `INTERNAL`](INTERNAL_operator.md), которое создаст объект класса `GetIP` (если у класса есть package, то в названии класса нужно также указывать package) и вызовет у него метод `executeInternal`. Исходный код этого класса:

#### GetIP.java
```java
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;

public class GetIP extends InternalAction {

    public GetIP(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            findProperty("ip").change(InetAddress.getLocalHost().toString(), context);
        } catch (UnknownHostException | ScriptingErrorLog.SemanticErrorException ignored) {
        }
    }
}
```

Сначала действие считывает при помощи встроенного класса `InetAddress` параметры сервера. После этого находит локальное свойство `ip`, объявленное в том же модуле, что и действие, и записывает в него при помощи метода `change` полученное значение.

Существует также альтернативный способ задания данного свойства:

```lsf
getIPFusion INTERNAL <{ findProperty("ip").change((Object)java.net.InetAddress.getLocalHost().toString(), context); }>;
showIPFusion 'Показать имя компьютера (Fusion)' {
    getIPFusion();
    MESSAGE ip();
}

EXTEND FORM info
    PROPERTIES() showIPFusion
;
```

Платформа сама генерирует нужный класс, добавляя туда заданный код, а затем компилирует его при помощи компилятора [Janino](https://janino-compiler.github.io/janino/). Преимущество такого подхода в том, что при сборке проекта не потребуется отдельный шаг с компиляцией Java кода. Однако, такой подход имеет ряд существенных ограничений и может использоваться только в самых простых случаях.

## Пример 2

### Условие

Нужно реализовать действие, которое посчитает наибольший общий делитель двух целых чисел.

### Решение

```lsf
gcd = DATA LOCAL INTEGER();
calculateGCD 'Рассчитать НОД' INTERNAL 'CalculateGCD' (INTEGER, INTEGER);

FORM gcd 'НОД'
    OBJECTS (a = INTEGER, b = INTEGER) PANEL
    PROPERTIES 'A' = VALUE(a), 'B' = VALUE(b)

    PROPERTIES gcd(), calculateGCD(a, b)
;

NAVIGATOR {
    NEW gcd;
}
```

Основное отличие от предыдущего примера в том, что действие принимает на вход 2 параметра типа `INTEGER`. Это нужно учитывать при написании класса `CalculateGCD`. Вот его исходный код:

#### CalculateGCD.java
```java
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.math.BigInteger;
import java.sql.SQLException;

public class CalculateGCD extends InternalAction {

    public CalculateGCD(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        BigInteger b1 = BigInteger.valueOf((Integer)getParam(0, context));
        BigInteger b2 = BigInteger.valueOf((Integer)getParam(1, context));
        BigInteger gcd = b1.gcd(b2);
        try {
            findProperty("gcd[]").change(gcd.intValue(), context);
        } catch (ScriptingErrorLog.SemanticErrorException ignored) {
        }
    }
}
```
Чтение значений входных свойств происходит через метод `getParam`, в котором первым параметром передается 0-based индекс считываемого свойства. Этот метод возвращает объект класса `Object`, поэтому требуется явное приведение к типу.

## Пример 3

### Условие

Нужно реализовать действие, которое посчитает наибольший общий делитель двух целых чисел, но они задаются как свойства для объекта.

### Решение

```lsf
CLASS Calculation;
a = DATA INTEGER (Calculation);
b = DATA INTEGER (Calculation);
gcd = DATA INTEGER (Calculation);
calculateGCD 'Рассчитать НОД' INTERNAL 'CalculateGCDObject' (Calculation);

EXTEND FORM gcd
    OBJECTS c = Calculation
    PROPERTIES(c) a, b, gcd, calculateGCD GRID, NEW, DELETE
;
```

В этом примере необходимо сначала считать значения свойств для переданного объекта, а затем записать результат также в свойство с одним входом. Делается это следующим образом:

#### CalculateGCDObject.java
```java
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.math.BigInteger;
import java.sql.SQLException;

public class CalculateGCDObject extends InternalAction {

    public CalculateGCDObject(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataObject calculation = (DataObject)getParamValue(0, context);
            BigInteger a = BigInteger.valueOf((Integer)findProperty("a").read(context, calculation));
            BigInteger b = BigInteger.valueOf((Integer)findProperty("b").read(context, calculation));
            BigInteger gcd = a.gcd(b);
            findProperty("gcd[Calculation]").change(gcd.intValue(), context, calculation);
        } catch (ScriptingErrorLog.SemanticErrorException ignored) {
        }
    }
}
```

Сначала в переменную `calculation` записывается переданные объект. Для этого используется специальный метод `getParamValue`, который возвращает объект класса `DataObject`. В нем хранится идентификатор объекта и его класс. Именно он используется в дальнейшем для чтения и записи свойств, путем передачи его последним параметром в методы `read` и `change`. Если бы свойство принимало на вход несколько объектов, то их всех нужно было бы передавать последними параметрами.

В методе `findProperty` используется полное каноническое имя свойства, так как существует несколько объявленных свойств `gcd` в модуле. Если указать только имя, то будет выдана соответствующая ошибка о невозможности определения нужного свойства.

## Пример 4

### Условие

Нужно реализовать действие, при выполнении которого на клиентской машине будет 5 раз выдаваться звуковой сигнал.

### Решение

```lsf
beep INTERNAL 'Beep';
FORM beep 'Сигнал'
    PROPERTIES() beep
;

NAVIGATOR {
    NEW beep;
}
```

Java код действия, созданного при помощи оператора `INTERNAL`, выполняется в виртуальной машине сервера. Поэтому нельзя вызывать сигнал непосредственно в коде класса, наследуемого от `InternalAction`. Для этой цели существует метод `requestUserInteraction`, в который нужно передать класс, наследуемый от класса `ClientAction`.

#### Beep.java
```java
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class Beep extends InternalAction {

    public Beep(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String result = (String) context.requestUserInteraction(new ClientBeep(5));
        System.out.println(result);
    }
}
```

Система остановит выполнение кода на вызове этого метода, затем передаст `ClientBeep` (и все используемые им классы, которых нету в JRE) в клиентское приложение, сконструирует объект с переданными параметрами (в данном случае, одним числом 5) и вызовет у него метод `dispatch`. Исходный код класса `ClientBeep`:

#### ClientBeep.java
```java
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.awt.*;
import java.io.IOException;

public class ClientBeep implements ClientAction {
    
    int times;

    public ClientBeep(int times) {
        this.times = times;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        for (int i = 0; i < times; i++) {
            try {
                Thread.sleep(1000);
                Toolkit.getDefaultToolkit().beep();
            } catch (InterruptedException ignored) {
            }
        }
        return "succeed";
    }
}
```

Объект, который вернет метод `dispatch`, будет возвращен на сервер в виде результата выполнения метода `requestUserInteraction`. В данном примере в стандартную консоль сервера будет выведено сообщение `succeed`. Таким образом можно считывать на сервере результаты выполнения кода на клиенте.

Так как java код не может быть выполнен непосредственно в браузере, то это действие будет работать только в десктоп-клиенте.

## Пример 5

### Условие

Нужно реализовать действие, которое будет слушать определенной порт и вызывать соответствующее действие, при получении нового сообщения.

### Решение

```lsf
CLASS Message;
text 'Text' = DATA STRING (Message);
receivedMessage (STRING str) {
    NEW m = Message {
        text(m) <- str;
    }
    APPLY;
}

listenPort INTERNAL 'ListenPort' (STRING, INTEGER);
onStarted() + {
    listenPort('localhost', 9999);
}
```

Для реализации задачи можно использовать библиотеку [Apache Mina](https://mina.apache.org/). Ее необходимо добавить в _classpath_ сервера.

#### ListenPort.java
```java
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.EventThreadInfo;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.sql.SQLException;

public class ListenPort extends InternalAction {

    public ListenPort(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    ListenPortServer server;

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String host = (String) ((DataObject) getParamValue(0, context)).getValue();
        Integer port = (Integer) ((DataObject) getParamValue(1, context)).getValue();

        server = new ListenPortServer(context.getLogicsInstance(), host, port);
    }

    private class ListenPortServer extends MonitorServer {

        LogicsInstance logicsInstance;

        public ListenPortServer(LogicsInstance logicsInstance, String host, Integer port) {
            this.logicsInstance = logicsInstance;

            try {
                IoAcceptor acceptor = new NioSocketAcceptor();

                acceptor.getFilterChain().addLast( "logger", new LoggingFilter() );
                acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ))));

                acceptor.setHandler(new ListenPortHandler());

                acceptor.bind(new InetSocketAddress(host, port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getEventName() {
            return "listenPort";
        }

        @Override
        public LogicsInstance getLogicsInstance() {
            return logicsInstance;
        }

        private class ListenPortHandler extends IoHandlerAdapter {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                super.sessionOpened(session);
                ThreadLocalContext.aspectBeforeMonitor(ListenPortServer.this, EventThreadInfo.HTTP(ListenPortServer.this));
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                String str = message.toString();
                try (DataSession dataSession = logicsInstance.createSession()) {
                    LM.findAction("receivedMessage[STRING]").execute(dataSession, getStack(), new DataObject(str, StringClass.instance));
                }
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                super.sessionClosed(session);
                ThreadLocalContext.aspectAfterMonitor(EventThreadInfo.HTTP(ListenPortServer.this));
            }
        }
    }
}
```

Действие `listenPort` создает объект класса `ListenPortServer`, который при помощи `NioSocketAcceptor` начинает слушать заданный порт, а сообщения обрабатывает через обработчик `ListenPortHandler`.
Поскольку в качестве кодека используется `TextLineCodecFactory`, то в обработчик все сообщения приходят в виде строки. 
Для каждого такого сообщения он вызывает действие `receivedMessage`, передавая в качестве параметра полученную строку. 
