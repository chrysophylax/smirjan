# Third-Party Data

The meaning data bundled with smirjan, in
[`resources/smirjan/meanings/`](resources/smirjan/meanings/), is derived from
the datasets listed below. All of them are published under the
[Creative Commons Attribution 4.0 International License (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/),
which permits use, modification and redistribution, including for commercial
purposes, provided that the source is attributed.

The files are generated from pinned releases of these datasets by
[`tools/BuildMeaningData.java`](tools/BuildMeaningData.java). **Changes
made:** the generator retains only the columns that smirjan uses, restricts
the data to the concepts on the three meaning lists (for semantic shifts, to
shifts from those concepts), and converts it to smirjan's tab-separated
format. In `concepts.tsv` a leading "the " is removed
from WOLD glosses, whereas the list files keep the glosses as published. The
`rank` column of the list files is Concepticon's `RANK` for the
Leipzig-Jakarta and WOLD lists, and the published order for the Dolgopolsky
list. In
`shifts.tsv` the asterisk with which DatSemShift marks duplicate entries is
removed from glosses, and a shift recorded under both copies of an entry is
counted once.

## Concepticon

List, Johann Mattis & Tjuka, Annika & Blum, Frederic & Kučerová, Alžběta &
Ugarte, Carlos Barrientos & Rzymski, Christoph & Greenhill, Simon & Forkel,
Robert (eds.) 2025. *Concepticon v3.4.0. A Resource for the Linking of Concept
Lists.* Leipzig: Max Planck Institute for Evolutionary Anthropology.
<https://concepticon.clld.org>, CC BY 4.0.
Pinned at commit `918bc44e` of
[concepticon/concepticon-data](https://github.com/concepticon/concepticon-data) (tag v3.4.0).

Concepticon is the source of the meaning lists and of the concept glosses,
semantic fields, ontological categories, concept relations and semantic
shifts (`lpj.tsv`, `dlg.tsv`, `wlt.tsv`, `concepts.tsv`, `relations.tsv` and
`shifts.tsv`). The concept lists
used are the following, each as published in Concepticon:

- **Leipzig-Jakarta list:** Tadmor, Uri. 2009. Loanwords in the world's
  languages: Findings and results. In Martin Haspelmath & Uri Tadmor (eds.),
  *Loanwords in the world's languages: A comparative handbook*, 55–75. Berlin
  and New York: de Gruyter.
  (Concepticon list Tadmor-2009-100; also the source of `ANALYZABILITY_SCORE`.)
- **Dolgopolsky list:** Dolgopolsky, Aharon B. 1964. Gipoteza drevnejšego
  rodstva jazykovych semej Severnoj Evrazii s verojatnostej točky zrenija
  [A probabilistic hypothesis concerning the oldest relationships among the
  language families of Northern Eurasia]. *Voprosy Jazykoznanija* 2. 53–63. (Concepticon list Dolgopolsky-1964-15.)
- **Loanword Typology (WOLD) meaning list:** Haspelmath, Martin & Uri Tadmor
  (eds.). 2009. *Loanwords in the world's languages: A comparative handbook*.
  Berlin and New York: de Gruyter. See also <https://wold.clld.org>. (Concepticon list
  Haspelmath-2009-1460; also the source of `SIMPLICITY_SCORE`.)
- **Urban (2011):** Urban, Matthias. 2011. *Asymmetries in overt marking and
  directionality in semantic change.* Journal of Historical Linguistics 1(1).
  3–47. (Concepticon list Urban-2011-160; used for "derived" relations.)
- **Database of Semantic Shifts:** Zalizniak, Anna, Anna Smirnitskaya, Maksim
  Russo (Rousseau), Ilya Gruntov, Timur Maisak, Dmitry Ganenkov, Maria Bulakh,
  Maria Orlova, Marina Bobrik-Fremke, Oksana Dereza, Tatiana Mikhailova, Maria
  Bibaeva & Mikhail Voronov. 2024. *Database of Semantic Shifts.* Moscow:
  Institute of Linguistics, Russian Academy of Sciences.
  <https://datsemshift.ru>. (Concepticon list Zalizniak-2024-4583, the dump
  of 5 February 2024 converted to CLDF by Bocklage et al. 2024; the source of
  `shifts.tsv`, used by `--shift`.) The conversion is described in Bocklage,
  Katja, Anna Di Natale, Annika Tjuka & Johann-Mattis List. 2024. Representing
  the Database of Semantic Shifts by Zalizniak et al. from 2024 in
  Cross-Linguistic Data Formats. *Computer-Assisted Language Comparison in
  Practice* 7(1). 25–35. <https://doi.org/10.15475/calcip.2024.1.4>.

## NoRaRe

Tjuka, Annika, Robert Forkel & Johann-Mattis List. 2022. Linking norms,
ratings, and relations of words and concepts across multiple language
varieties. *Behavior Research Methods* 54. 864–884.
<https://doi.org/10.3758/s13428-021-01650-1>.

*Database of Cross-Linguistic Norms, Ratings, and Relations for Words and
Concepts* as CLDF dataset, v1.1. <https://norare.clld.org>, CC BY 4.0.
Pinned at commit `895b055b` of
[concepticon/norare-cldf](https://github.com/concepticon/norare-cldf) (tag v1.1).

NoRaRe is the source of the CLICS colexification communities recorded in
`concepts.tsv`. These communities originate from the following dataset:

- Rzymski, Christoph, Tiago Tresoldi, Simon J. Greenhill, Mei-Shin Wu et al.
  2020. The Database of Cross-Linguistic Colexifications, reproducible analysis
  of cross-linguistic polysemies. *Scientific Data* 7(1). 1–12. (NoRaRe dataset
  Rzymski-2020-1624.)
