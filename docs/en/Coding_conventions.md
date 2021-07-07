---
title: 'Coding conventions'
---

### Common

- Class names, module names, and namespace names begin with a capital letter. Other names (property, action, form, parameter, etc.) start with a lowercase letter. If the name consists of several words, then each subsequent word in the name begins with a capital letter.
    ```lsf
    MODULE MyModule;

    NAMESPACE MyNamespace;

    CLASS MyClass 'My Class';

    myProperty 'My Property' = DATA BOOLEAN (MyClass);
    myAction 'My Action' (MyClass o) {
        myProperty(o) <- TRUE;
    }

    FORM myForm
    ;
    ```

- Module, requirements and namespace declarations must be separated by blank lines:
    ```lsf
    MODULE MyModule;

    REQUIRE MyModule2;

    NAMESPACE MyNamespace;
    ```

- `=`, `<`, `>`, `<=`, `>=`, `+`, `-` operators are separated on both sides by spaces.
    ```lsf
    value(x) = 324
    ```

- A space is placed after the comma. There should be no space before the comma.
    ```lsf
    f(x, y, z) = g(x, y, z) + h(x, y);

    FORM test
        OBJECTS a = Class1, b = Class2
        PROPERTIES VALUE(a), VALUE(b), name(a), name(b)
    ;
    ```

- Every new property or action declaration starts on a new line. If there is another code after the `;`, then a space is placed after the character.
    ```lsf
    runAction1 (X x) = { f(x) <- 1; g(x) <- 1; }
    ```

- When declaring a property or action, a space is inserted between the name or type and the symbol `(`. When using a property, there is no space.
    ```lsf
    nameProperty (MyClass o) = name(property(o));
    myProperty = DATA STRING[10] (MyClass);
    ```
    
### Actions
  
- Each "logically nested" line should have an offset by exactly 4 spaces. Actions are considered nested after `IF`, `FOR`, `WHILE`, as well as after the property declaration itself:
    ```lsf
    IF x = 1 THEN
        f(a) <- TRUE;
    ```

- Spaces are placed before and after symbols `{` and `<-`.
    ```lsf
    FOR f(a) DO {
        x(a) <- TRUE;
    }
    ```
    or
    ```lsf
    FOR f(a) DO { x(a) <- TRUE; }
    ```
    
### Forms

- Object declarations are separated by an empty line.
    ```lsf
    FORM test
        OBJECTS a = Object
        PROPERTIES(a) VALUE

        OBJECTS b = Object
        PROPERTIES(b) VALUE
    ;   
    ```
