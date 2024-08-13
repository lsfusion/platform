---
title: 'Comments'
---

Comments in a programming language allow you to add textual explanations and notes directly into the code without affecting its execution. In the **lsFusion** language, there are two types of comments: line comments and block comments.

### Line Comments

Line comments are marked by two slashes `//`. Everything that follows these symbols until the end of the line is treated as a comment and is ignored by the interpreter.

#### Examples

```lsf
// The version number should be increased when the API changes
apiVersion() = 23;

apiVersion() = 23; // The version number should be increased when the API changes
```

### Block Comments

Block comments allow for more detailed explanations and can also be used to comment out portions of code within a single line. Block comments are enclosed between the sequences `/*` and `*/`. Everything between these markers is treated as a comment.

#### Examples

```lsf
REQUIRE System, Scheduler, Service, Backup, /*Eval, Chat,*/ SystemEvents;

/*
The version number should be increased when the API changes.
This is necessary for correct operation.
*/
apiVersion() = 23;
```