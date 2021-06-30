---
title: 'How-to: Binding properties'
---

## Example 1

### Task

The concepts of Country, Region and City are defined. A region and a city are always located in a certain country. A city may have a specified region, but need not.

```lsf
CLASS Country 'Country';
name 'Name' = DATA ISTRING[100] (Country) IN id;

CLASS Region 'Region';
name 'Name' = DATA ISTRING[100] (Region) IN id;

country 'Country' = DATA Country (Region) NONULL DELETE;
nameCountry 'Country' (Region r)= name(country(r)) IN id;

CLASS City 'City';
name 'Name' = DATA ISTRING[100] (City);

country 'Country' = DATA Country (City) NONULL DELETE;
nameCountry (City c) = name(country(c));

region 'Region' = DATA Region (City);
nameRegion (City c) = name(region(c));

FORM cities 'Cities'
    OBJECTS c = City
    PROPERTIES(c) name, nameCountry, nameRegion, NEW, DELETE
;

NAVIGATOR {
    NEW cities;
}
```

We need to define the logic such that a city can only be linked to a region belonging to the country it is in.

### Solution

```lsf
CONSTRAINT country(City c) != country(region(c)) CHECKED BY region[City] MESSAGE 'The country of the city region must match the country of the city';
```

The [`CONSTRAINT` statement](CONSTRAINT_statement.md) defines a condition that must always be `NULL`. In this case, the constraint based on this condition will be violated if there is a country defined for the region, a region for the city, and a country for the region, and the region's country is not the same as the city's country. The `CHECKED BY` block indicates that when you select a region in a dialog, the regions will be filtered by default so as not to violate this constraint. It should be noted that if no country has yet been set for the given city then the condition will always be `NULL`, and all existing regions will be shown in the dialog.

## Example 2

### Task

Similar to [**Example 1**](#example-1).

We need to make it so that the country is automatically filled when a region is chosen.

### Solution

```lsf
WHEN LOCAL CHANGED(region(City c)) AND NOT CHANGED(country(c)) DO {
    country(c) <- country(region(c));
}
```

It should be noted that after the user has chosen a region and a country is set, when the dialog is called again, only the regions of the selected country will be displayed. If the user wants to see all the regions again, he will need to clear the country first. The expression checks that the country has not changed is added to provide the following behavior: if the cities are changed by external actions that change both the country and the region, then the country should not be changed with this event.

## Example 3

### Task

Similar to [**Example 1**](#example-1).

We need to create the same kind of association between the region and the city, but in such a way that a region cannot be selected until a country has been set first.

### Solution

```lsf
CONSTRAINT region(City c) AND NOT country(c) = country(region(c)) CHECKED BY region[City] MESSAGE 'The country of the city region must match the country of the city';
```

The difference from the first example is that the new condition will also be true if a region is set and a country is not. Therefore, in this case the dialog will not display any regions.

## Example 4

### Task

Similar to [**Example 1**](#example-1).

If the user first selects a region, and then a country that does not correspond to the original region, the user will receive an error message when trying to save.

We need to make it so that the region is dropped when a new country is selected, if the region does not correspond to that country.

### Solution

```lsf
WHEN LOCAL CHANGED(country(City c)) AND country(c) != country(region(c)) DO {
    region(c) <- NULL;
}
```

## Example 5

### Task

Similar to [**Example 1**](#example-1).

The solution to the first example has one drawback. The system will not allow you to change the country of a particular region, if the city links to it. The constraint will be violated.

We need to make it so that when a region's country is changed, it also changes automatically for all that region's cities.

### Solution

```lsf
// 1 option
WHEN SETCHANGED(country(region(City c))) DO
    country(c) <- country(region(c));

// 2 option
WHEN SETCHANGED(country(Region r)) DO
    country(City c) <- country(r) WHERE region(c) = r;
```
