---
title: 'Icon assignment mechanism'
---

In the lsFusion platform, developers can manually assign icons to UI elements corresponding to [properties](Properties.md), [actions](Actions.md), [forms](Forms.md), [containers](Form_design.md#containers), and [navigator elements](Navigator.md). You can specify either an image file path or icon font CSS classes.

In addition, the platform provides a mechanism that can automatically assign suitable icons for these elements. The mechanism analyzes their names and captions and selects the most appropriate icons from the available sets.

## Manual assignment {#manual}

An icon is set using a string literal, which can be specified in one of the following ways:

- File path: `'path/to/image.png'` (relative to the `images` directory)
- Icon font CSS classes: `'fa fa-user'` (Font Awesome), `'bi bi-person'` (Bootstrap Icons)

::::info
Icon font CSS classes are CSS classes for displaying vector icons from special fonts. Such icons scale without loss of quality and take up less space than raster images.
::::

The platform supports free icons from two popular sets: **Font Awesome** and **Bootstrap Icons**.

## Automatic assignment {#auto}

::::info
Automatic icon assignment works only if the `Icon.lsf` module is added to the project.
::::

### General logic

1. Keywords are extracted from the element name or caption.
2. Candidates are selected by these keywords; the icon with the highest match rank is chosen.
3. If the rank is not lower than the threshold for this element type, the found icon is used.
4. Otherwise, the default icon for this element type is applied (if its use is enabled).

### Determining search keywords

Keywords are formed from the caption and/or the element name according to the following rules:

1. The caption is used first. If it is localizable, its English variant is taken.
2. The text is split into keywords; short words can be removed if necessary.
3. If there is no caption, or its processing does not produce a keyword list, the element name is used and processed in the same way.

### Settings and parameters

Automatic icon assignment is configured via a set of [working parameters](Working_parameters.md#iconSettings).

Parameters that change ranking thresholds:

| Parameter                                 | Scope                           | Default threshold |
|-------------------------------------------|---------------------------------|-------------------|
| `defaultAutoImageRankingThreshold`        | All auto-icons                  | `0.0`             |
| `defaultNavigatorImageRankingThreshold`   | Navigator elements              | `0.1`             |
| `defaultContainerImageRankingThreshold`   | Containers and forms            | `0.6`             |
| `defaultPropertyImageRankingThreshold`    | Properties and actions          | `0.8`             |

Parameters to enable/disable default icons:

| Parameter                | Element type             | Default value |
|--------------------------|--------------------------|---------------|
| `defaultNavigatorImage`  | Navigator elements       | `true`        |
| `defaultContainerImage`  | Containers and forms     | `false`       |
| `defaultPropertyImage`   | Properties and actions   | `false`       |

By default, automatic icon assignment is enabled only for navigator elements.

