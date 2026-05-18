---
title: 'Expression'
---

An *expression* is a combination of [property operators](Property_operators.md) and [parameters](Properties.md). When an expression is evaluated sequentially in [priority](Operator_priority.md) order, all the operators are executed.

The result of that execution will be either a [property](Properties.md) or a parameter (in the case of single-parameter expression). Their value shall be called the *value* of the expression.

An expression can be described by the following set of recursive rules:

|Rule|Description|
|---|---|
|`expression` := `parameter` \| `constant` \| `prefixOperator`|A single parameter, [constant](Constant.md), or non-arithmetic prefix operator|
|`expression` := `prefixArithmOp expression`|A unary arithmetic prefix operator, with the expression passed to it as an operand|
|`expression` := `expression postfixOp`|A unary postfix operator, with the expression passed to it as an operand|
|`expression` := `expression binaryOp expression`|A binary operator with the expressions passed to it as operands|
|`expression` := `( expression )`|Expression in parentheses|

An expression cannot include [context-independent](Property_operators.md#contextindependent) property operators.

### Using actions inside expressions

Inside an action body, an expression can also use a call to an [action](Actions.md) that returns a result, treating it as a property. In this case the parentheses first list the values of the action's input parameters, and then the new local parameters that become the names of the result's parameters for further use in the expression.

Such a call is equivalent to the sequential execution of:

- creation of a [local property](Data_properties_DATA.md#local) with the signature of the action's result;
- call of the action with the result written into that local property;
- substitution of that local property into the expression.

Action calls in expressions are only allowed inside an action body.

### Examples

```lsf
CLASS Team;

wins(team) = DATA INTEGER(Team);
ties(team) = DATA INTEGER(Team);

// The number of points received by the team for the matches played
points(Team team) = wins(team) * 3 + ties(team); 
// In this case, the expression is written to the right of the equal sign. It defines a new property called points.
// When calculating the expression, two JOIN operators are first executed: wins(team) and ties(team), substituting 
// the team parameter in the wins and ties properties. Then the multiplication operator will be executed, 
// which will build a property that returns a number equal to the product of the return value of wins(team) 
// and the number 3. Then the addition operator will be executed, which will create a property that sums the return
// values (wins(team) * 3) and ties(team). The resulting property will be the result of the expression.

CLASS Game;
CLASS BonusGame : Game;

// The number of points per game. If the game is bonus, then 3, otherwise 2.
gamePoints(Game game) = 2 (+) (1 IF game IS BonusGame); 
// In this example, the order of execution of the operators will be as follows: IS, IF, (+)

CLASS Item;
price(Item i) = DATA INTEGER (Item);
label(Item i) = DATA STRING[100] (Item);

priceBucket (INTEGER p)  {
    IF p > 1000 THEN RETURN 'high';
    IF p > 100 THEN RETURN 'mid';
    RETURN 'low';
}

// call of action priceBucket inside an expression in the body of another action
labelItem (Item i)  {
    label(i) <- 'Bucket: ' + priceBucket(price(i));
}
```

