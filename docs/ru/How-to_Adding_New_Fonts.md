---
title: 'How-to: Добавление новых шрифтов'
---

Платформа lsFusion гарантированно поддерживает в JasperReports следующие шрифты: `DejaVu Sans`, `DejaVu SansMono`, `DejaVu Serif`, `Liberation Mono`, `Liberation Sans`, `Liberation Serif`, `Noto Sans`, `Noto Serif`.
`DejaVu Sans` установлен как шрифт по-умолчанию.

Можно использовать и другие шрифты, если вы уверены, что они есть в операционной системе. Если их может не быть в операционной системе, для гарантированной работы следует добавить их в проект.

### Алгоритм добавления нового шрифта

В папку `resources` проекта необходимо добавить xml и файлы разновидностей шрифта `normal`, `bold`, `italic`, `boldItalic`.

Например, добавим Шрифт `Arial`. В папке проекта `resources` создадим папку `fonts`, в ней - папку `arial`, в неё положим файл `Arial.xml` и файлы разновидностей шрифта `normal`, `bold`, `italic`, `boldItalic`.

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

После этого в файл `jasperreports_extensions.properties` необходимо добавить строку с путём к файлу `Arial.xml`
```properties
net.sf.jasperreports.extension.simple.font.families.Arial=fonts/arial/Arial.xml
```

Файл `jasperreports_extensions.properties` также должен распологаться в папке `resources` проекта. Если такого нет, его надо скопировать из api (`projectDir\platform\api\src\main\resources`).
Строки с новыми шрифтами для удобства лучше расположить вверху файла.