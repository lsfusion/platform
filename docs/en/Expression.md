---
title: 'Expression'
sidebar_label: Overview
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

### Examples

```lsf
CLASS Team;

wins(team) = DATA INTEGER(Team);
ties(team) = DATA INTEGER(Team);

points(Team team) = wins(team) * 3 + ties(team); // The number of points received by the team for the matches played
// In this case, the expression is written to the right of the equal sign. It defines a new property called points.
// When calculating the expression, two JOIN operators are first executed: wins(team) and ties(team), substituting the team parameter in the wins and ties properties
// Then the multiplication operator will be executed, which will build a property that returns a number equal to the product of the return value of wins(team) and the number 3
// Then the addition operator will be executed, which will create a property that sums the return values (wins(team) * 3) and ties(team)
// The resulting property will be the result of the expression

CLASS Game;
CLASS BonusGame : Game;

gamePoints(Game game) = 2 (+) (1 IF game IS BonusGame); // The number of points per game. If the game is bonus, then 3, otherwise 2.
// In this example, the order of execution of the operators will be as follows: IS, IF, (+)
```

