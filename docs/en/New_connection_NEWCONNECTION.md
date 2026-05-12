---
title: 'New external connections (NEWCONNECTION)'
---

[External connections](Access_to_an_external_system_EXTERNAL.md) opened while calling an external system can be reused across several successive calls to the same endpoint. This saves the cost of establishing the connection and also lets calls share state that the connection itself holds — SQL temporary tables and session variables, an open TCP socket and its read buffer, the current position inside a DBF file, and so on.

The platform allows scoping a block inside which every call to the same endpoint reuses the previously opened connection. Typical cases — a series of SQL queries against the same external database, a series of operations over DBF files in a single directory, or a TCP session of several messages.

Every connection opened inside the block is closed when the block exits, regardless of whether the inner action completed normally or threw.

The block runs in the current [session](New_session_NEWSESSION_NESTEDSESSION.md); nothing besides the external-connection reuse policy is changed.

### Language

To create an external connection reused across several external-system calls, use the [`NEWCONNECTION` operator](NEWCONNECTION_operator.md).

### Examples

```lsf
syncStock (STRING sku) {
    NEWCONNECTION {
        // one connection serves both queries
        EXTERNAL SQL 'jdbc:postgresql://erp/main' EXEC 'UPDATE stock SET qty = qty - 1 WHERE sku = $1' PARAMS sku;
        EXTERNAL SQL 'jdbc:postgresql://erp/main' EXEC 'INSERT INTO audit (msg) VALUES ($1)' PARAMS 'sync';
    }
    // the erp/main connection is closed here
}
```
