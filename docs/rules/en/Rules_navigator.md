---
slug: "/Rules_navigator"
title: 'Rules: navigator'
---

## Navigator rules

1. A folder whose children should appear only when the folder
   is selected MUST place those children in a different window
   than the folder itself (typically `WINDOW toolbar`). In a
   horizontal toolbar such as `System.root`, a folder that keeps
   its children in its own window cannot switch anything — they
   are shown flattened next to it and selecting the folder does
   nothing. A vertical toolbar instead renders same-window
   children as a nested group under the folder, so there the
   separate window is not required.
