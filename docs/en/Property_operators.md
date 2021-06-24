---
title: 'Property operators'
sidebar_label: Overview
---

*Property operator* is a syntax construct that describes an [operator](Property_operators_paradigm.md) creating a [property](Properties.md). 

Property operators can be divided into *context dependent* and *context independent*.

### Context dependent operators

Context dependent property operators can be part of [expressions](Expression.md) and use common parameters (context). These operators are:

-   [Arithmetic operators](Arithmetic_operators.md)
-   [`[]` operator](Brackets_operator.md)
-   [`ACTIVE TAB` operator](ACTIVE_TAB_operator.md)
-   [`AGGR` operator](AGGR_operator.md)
-   [`CASE` operator](CASE_operator.md)
-   [`CLASS` operator](CLASS_operator.md)
-   [`CONCAT` operator](CONCAT_operator.md)
-   [`EXCLUSIVE` operator](EXCLUSIVE_operator.md)
-   [`IF` operator](IF_operator.md)
-   [`IF ... THEN` operator](IF_..._THEN_operator.md)
-   [`JOIN` operator](JOIN_operator.md)
-   [`MAX` operator](MAX_operator.md)
-   [`MIN` operator](MIN_operator.md)
-   [`MULTI` operator](MULTI_operator.md)
-   [`OVERRIDE` operator](OVERRIDE_operator.md)
-   [`PARTITION` operator](PARTITION_operator.md)
-   [`GROUP` operator](GROUP_operator.md) (without `BY` block)
-   [`PREV` operator](PREV_operator.md)
-   [`RECURSION` operator](RECURSION_operator.md)
-   [`STRUCT` operator](STRUCT_operator.md)
-   [`UNGROUP` operator](UNGROUP_operator.md)
-   [Type conversion operator](Type_conversion_operator.md)
-   [`AND`, `OR`, `NOT`, `XOR` operators](AND_OR_NOT_XOR_operators.md)
-   [`IS`, `AS` operators](IS_AS_operators.md)
-   [Change operators](Change_operators.md)
-   [Comparison operators](Comparison_operators.md)

### Context independent operators {#contextindependent}

Context independent property operators differ from the context dependent ones in that they do not just use the upper parameters but define the parameters themselves. As a result, they cannot be part of expressions and can only be used in the [`=` statement](=_statement.md) and the [`JOIN` operator](JOIN_operator.md): 

-   [`DATA` operator](DATA_operator.md)
-   [`GROUP` operator](GROUP_operator.md) (with `BY` block)
-   [`FORMULA` operator](FORMULA_operator.md)
-   [`ABSTRACT` operator](ABSTRACT_operator.md)
-   [Object group operators](Object_group_operator.md)
