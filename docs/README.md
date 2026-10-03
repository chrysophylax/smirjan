# smirjan Documentation

smirjan generates words for a constructed language from a description of the
language's phonology. The description is kept in a plain-text `.def` file,
which lists the phonemes, the shapes a syllable may take and the rules that
words must obey; smirjan reads the file and prints as many distinct words as
are requested.

The chapters are meant to be read in order. The first starts from a
definition only three lines long, and each later chapter adds a few keys to
what came before, so that by the end the same mechanisms describe a complete
language. Most chapters come with runnable example files in
[`examples/`](examples/), named after the chapter, and all the sample output
quoted in these pages was produced by running them or the two complete
definitions in the repository's top-level `examples/` directory.

1. [Getting Started](01-getting-started.md): building, running, the first definition, seeds
2. [Phonemes, Classes and Frequency](02-phonemes-and-frequency.md): classes, rank order, Gusein-Zade and Yule, explicit weights
3. [Syllable Shapes](03-syllable-shapes.md): shapes, literals, optional elements, word-position shapes
4. [Restrictions](04-restrictions.md): exceptions, numbered slots for clusters and geminates
5. [Word Length and Weight](05-word-length-and-weight.md): syllable counts, morae
6. [Stress](06-stress.md): fixed and weight-sensitive stress, secondary stress, marks
7. [Tone and Pitch Accent](07-tone-and-pitch.md): syllable tone, mora tone, contours, pitch accent
8. [Filters and Rejects](08-filters-and-rejects.md): rewriting and discarding words with patterns
9. [Cluster Tables](09-cluster-tables.md): controlling adjacent phonemes
10. [Output Formats and Meaning Lists](10-output-and-meaning-lists.md): `--tsv`, `--assign`, duplicates, exit codes
11. [Compounds](11-compounds.md): dvandva and determinative compounds, head order, meanings mined from Concepticon
12. [A Complete Language](12-a-complete-language.md): Var Ysalenn, step by step
13. [Reference](reference.md): every key and flag on one page

Chapters 6, 7 and 11 can be skipped by readers whose language has no stress,
tone or compounds, because smirjan produces these only when a definition asks
for them.

The meaning data bundled with smirjan comes from Concepticon and NoRaRe, both
licensed under CC BY 4.0; the credits are in [NOTICE.md](../NOTICE.md).
