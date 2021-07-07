---
title: 'How-to: Property extension'
---

The classic approach for implementing polymorphism can look as follows:

Let's create an abstract class `Shape` with an abstract property `square`:

```lsf
CLASS ABSTRACT Shape;
square 'Area' = ABSTRACT DOUBLE (Shape);
```

Now, let's create classes `Rectangle` and `Circle` inherited from `Shape`:

```lsf
CLASS Rectangle : Shape;
width 'Width' = DATA DOUBLE (Rectangle);
height 'Height' = DATA DOUBLE (Rectangle);

CLASS Circle : Shape;
radius 'Circle radius' = DATA DOUBLE (Circle);
```

Define the implementation of the abstract property `square` for the created classes:

```lsf
square(rectangle) += width(rectangle) * height(rectangle);
square(circle) += radius(circle) * radius(circle) * 3.14;
```

Let's assume that we need to make it so that in certain cases we can override the method of calculating the area for the `Circle` class. In this case, we can use an abstract property, whose implementation can be changed in a different module, to add a kind of "entry point" to the line defining the implementation of area for a circle :

```lsf
overSquareCircle 'Overridden area' = ABSTRACT DOUBLE (Circle);
square(circle) += OVERRIDE overSquareCircle(circle), (radius(circle) * radius(circle) * 3.14);
```

If the `overSquareCircle` property is not implemented in any module, its value will always be `NULL` and the base area calculation mechanism will be used. To change the calculation method to be used, you can define a different implementation in a certain `MyShape` module.

```lsf
MODULE MyShape;

REQUIRE Shape;

overSquareCircle (circle) += radius(circle) * radius(circle) * 3.14159265359; // using a formula with higher accuracy
```

Note that you can use any other expressions instead of the [`OVERRIDE` operator](OVERRIDE_operator.md). In particular, [`(+)` and `(-)` operators](Arithmetic_operators_+_-_etc.md) may be the most frequently used.
