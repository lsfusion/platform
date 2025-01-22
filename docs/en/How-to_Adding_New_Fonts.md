---
title: 'How-to: Adding New Fonts'
---

The lsFusion platform guarantees support for the following fonts in JasperReports: `DejaVu Sans`, `DejaVu SansMono`, `DejaVu Serif`, `Liberation Mono`, `Liberation Sans`, `Liberation Serif`, `Noto Sans`, `Noto Serif`. The default font is `DejaVu Sans`.

You can also use other fonts, as long as they are available on the operating system. If they may not be present on the operating system, you should add them to the project to ensure proper functionality.

### Steps to Add a New Font

You need to add the XML file and the font variant files for `normal`, `bold`, `italic`, and `boldItalic` to the project's `resources` folder.

For example, to add the `Arial` font, create `fonts` directory in the `resources` directory of the project, then create `arial` subdirectory, where need to place the `Arial.xml` file and the font variant files: `normal`, `bold`, `italic`, and `boldItalic`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<fontFamilies>
    <fontFamily name="Arial">
        <normal>fonts/arial/arial.ttf</normal>
        <bold>fonts/arial/arialbd.ttf</bold>
        <italic>fonts/arial/ariali.ttf</italic>
        <boldItalic>fonts/arial/arialbi.ttf</boldItalic>
        <pdfEmbedded>true</pdfEmbedded>
     </fontFamily>
</fontFamilies>
```

Next, add the following line to the `jasperreports_extensions.properties` file with the path to the `Arial.xml` file:
```properties
net.sf.jasperreports.extension.simple.font.families.Arial=fonts/arial/Arial.xml
```

The `jasperreports_extensions.properties` file should also be located in the project's `resources` folder. If it does not exist, you can copy it from the api (`projectDir\platform\api\src\main\resources`). For convenience, it's better to place the lines with the new fonts at the top of the file.