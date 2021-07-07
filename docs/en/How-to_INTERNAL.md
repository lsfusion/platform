---
title: 'How-to: INTERNAL'
---

## Example 1

### Task

We need to implement an action that will display a message with the server's name and IP address.

### Solution

```lsf
ip = DATA LOCAL TEXT();
getIPJava INTERNAL 'GetIP';
showIPJava 'Show computer name (Java)' {
    getIPJava();
    MESSAGE ip();
}

FORM info 'Information'
    PROPERTIES() showIPJava
;

NAVIGATOR {
    NEW info;
}
```

To solve the task, create an action using the [`INTERNAL` operator](INTERNAL_operator.md) which will generate an object of the `GetIP` class (if the class has a package, then you must also specify "package" in the class name) and will call the `executeInternal` method. The source code for this class will be as follows:

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

First, the action reads the server parameters using the built-in `InetAddress` class. Then it finds the local property `ip` declared in the same module as an action and writes the resulting value to it using the `change` method.

There is also an alternative way to set this property:

```lsf
getIPFusion INTERNAL <{ findProperty("ip").change((Object)java.net.InetAddress.getLocalHost().toString(), context); }>;
showIPFusion 'Show computer name (Fusion)' {
    getIPFusion();
    MESSAGE ip();
}

EXTEND FORM info
    PROPERTIES() showIPFusion
;
```

The platform will generate the target class, insert the specified code into it and then compile it using the Janino [compiler](https://janino-compiler.github.io/janino/). The advantage of this approach is that building the project does not require a dedicated step for compiling the Java code. However, the approach has a number of significant limitations and can be used only in the simplest cases.

## Example 2

### Task

We need to implement an action that calculates the maximum common divisor of the two integers.

### Solution

```lsf
gcd = DATA LOCAL INTEGER();
calculateGCD 'Calculate GCD' INTERNAL 'CalculateGCD' (INTEGER, INTEGER);

FORM gcd 'GCD'
    OBJECTS (a = INTEGER, b = INTEGER) PANEL
    PROPERTIES 'A' = VALUE(a), 'B' = VALUE(b)

    PROPERTIES gcd(), calculateGCD(a, b)
;

NAVIGATOR {
    NEW gcd;
}
```

The key difference from the previous example is that the action has two `INTEGER` arguments. Keep this in mind when writing your own `CalculateGCD` class. Here is the source code:

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

Values of input properties are read using the `getParam` method, in which the first parameter passed is a 0-based index of the property to be read. This method returns an object of class `Object`, so explicit type casting is required.

## Example 3

### Task

We need to implement an action that calculates the greatest common divisor of two integers, but they are specified as properties for an object.

### Solution

```lsf
CLASS Calculation;
a = DATA INTEGER (Calculation);
b = DATA INTEGER (Calculation);
gcd = DATA INTEGER (Calculation);
calculateGCD 'Calculate GCD' INTERNAL 'CalculateGCDObject' (Calculation);

EXTEND FORM gcd
    OBJECTS c = Calculation
    PROPERTIES(c) a, b, gcd, calculateGCD GRID, NEW, DELETE
;
```

In this example we first need to read the values of the properties for the passed object, and then to write the result to a property with one input. This is done as follows:

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

The object that is passed is first written to the variable `calculation`. This is done using the special `getParamValue` method, which returns an object of class `DataObject`. This stores the object's ID and class. It will then be used to read and write properties, by passing it as the last parameter to the `read` and `change` methods. If the property took several objects as input, every of them would need to be passed as the last parameter.

The full canonical name of the property is used in the `findProperty` method, because several `gcd` properties are declared in the module. If you only specify the name, then a corresponding error will be issued saying it is impossible to determine the required property.

## Example 4

### Task

We need to implement an action that will generate a sound signal 5 times on the client machine.

### Solution

```lsf
beep INTERNAL 'Beep';
FORM beep 'Signal'
    PROPERTIES() beep
;

NAVIGATOR {
    NEW beep;
}
```

The Java code for an action created using the `INTERNAL` operator, runs on the server's virtual machine. So the signal cannot be called directly from the code of a class that inherits from `InternalAction`. For this purpose there is a method called `requestUserInteraction`, which must be passed a class that inherits from class `ClientAction`.

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

The system halts code execution when this method is called, then passes `ClientBeep` (and all classes it uses that are not present in JRE) to the client application, constructs an object with the parameters passed (in this case just the number 5), and calls its `dispatch` method. Source code of class `ClientBeep`:

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
The object returned by the `dispatch` method is returned to the server as the result of executing the `requestUserInteraction` method. In this example, the message `succeed` will be displayed on the server's standard console. Thus, results of code execution on the client can be read on the server.

Since java code cannot be executed directly in the browser, this action will only work with a desktop client.
