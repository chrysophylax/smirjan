# smirjan documentation

smirjan generates words for a constructed language from a description of its
phonology. You write the description in a `.def` file, and smirjan prints as
many distinct words as you ask for.

The chapters start simple and build on each other. Each one introduces a few
keys, with a runnable example in [`examples/`](examples/). All sample output
in these pages was produced by running those files.

1. [Getting started](01-getting-started.md): building, running, the first definition, seeds
2. [Phonemes, classes and frequency](02-phonemes-and-frequency.md): classes, rank order, Gusein-Zade and Yule, explicit weights
3. [Syllable shapes](03-syllable-shapes.md): shapes, literals, optional elements, word-position shapes
4. [Restrictions](04-restrictions.md): exceptions, numbered slots for clusters and geminates
5. [Word length and weight](05-word-length-and-weight.md): syllable counts, morae
6. [Stress](06-stress.md): fixed and weight-sensitive stress, secondary stress, marks
7. [Tone and pitch accent](07-tone-and-pitch.md): syllable tone, mora tone, contours, pitch accent
8. [Filters and rejects](08-filters-and-rejects.md): rewriting and discarding words with patterns
9. [Cluster tables](09-cluster-tables.md): controlling adjacent phonemes
10. [Output formats and meaning lists](10-output-and-meaning-lists.md): `--tsv`, `--assign`, duplicates, exit codes
11. [A complete language](11-a-complete-language.md): Var Ysalenn, step by step
12. [Reference](reference.md): every key and flag on one page

Stress and tone are optional. A language has none unless its `.def` file asks
for them.
