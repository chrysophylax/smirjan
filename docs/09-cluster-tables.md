# 9. Cluster Tables

A **cluster table** states, for pairs of adjacent phonemes, whether each pair
is allowed, forbidden or replaced by something else. Because one table covers
every combination of the phonemes in its rows and columns, it can express in a
few lines what would otherwise take many separate rules, which makes it the
natural tool for consonant clusters, vowel sequences and assimilation.

Both the idea and the notation are taken from William Annis's
[Lexifer](https://lingweenie.org/conlang/lexifer/).

## Notation

```
% a  i  u
a +  +  o
i -  +  uu
u -  -  +
```

A table begins with a line starting with `%`, and the rest of that line lists
the **second** phoneme of each pair. Each following line begins with the
**first** phoneme of a pair and continues with one cell per column, so the
cell at row *a*, column *u* governs the sequence *au*. The table ends at a
blank line, at a new `key: value` line or at another `%` line; comment lines
do not end it.

Each cell is one of:

| cell | meaning |
|---|---|
| `+` | the pair is allowed |
| `-` | the pair is forbidden; any word containing it is discarded |
| anything else | the pair is replaced by this text |

In the table above, *ai* is allowed, *au* becomes *o*, *ia* is forbidden and
*iu* becomes *uu*.

A pair that no table mentions is allowed, so a table only needs to list the
phonemes whose combinations matter. A file can contain several tables, and if
two of them cover the same pair, the later one takes precedence.

## What Counts as a Pair

Tables check **every pair of adjacent phonemes in the word**, including pairs
that span a syllable boundary. That's usually what matters for clusters, which
mostly arise where the coda of one syllable meets the onset of the next.

A pair consists of two phonemes rather than two characters. If `tʃ` is defined
as a phoneme, the table sees the pair `tʃ` + `a` and never `ʃ` + `a`.

Row and column labels can also be class names, which expand to every phoneme
of the class:

```
% T
N -        # no nasal directly before a member of T
```

## Example: Nasal Assimilation

([`examples/09-clusters.def`](examples/09-clusters.def))

```
C: t k p s l m n
N: m n
V: a i u e o

syllable: CV CVN
word-syllables: 2 3

% p  t  k  s  l
m +  nt ŋk -  -
n mp +  ŋk +  ll
```

Read row by row, the table says that *m* before *t* becomes *nt* and before
*k* becomes *ŋk*, and that *m* may not precede *s* or *l* at all; *n* before
*p* becomes *mp*, before *k* becomes *ŋk*, and before *l* becomes *ll*. The
effect on 2000 words with and without the table is as follows:

| | mp | mt | mk | ms | ml | np | nt | nk | ns | nl | ŋk | ll |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| without | 113 | 221 | 145 | 71 | 63 | 45 | 81 | 43 | 24 | 17 | 0 | 0 |
| with | 153 | 0 | 0 | 0 | 0 | 0 | 305 | 0 | 28 | 0 | 189 | 19 |

The clusters *mt*, *mk*, *np* and *nk* disappear because they are replaced,
which is why *mp*, *nt* and *ŋk* become more frequent; *ms* and *ml* disappear
because the words that contained them were discarded. As a result, a nasal
always agrees in place of articulation with a following stop, as in the first
six such words among the first hundred generated:

```
kintu lento seŋka kempi piŋkatim kusaŋka
```

## Substitutions in Detail

How a replacement fits back into the word depends on its length. A replacement
of **two phonemes** occupies the two places of the original pair. With `n` +
`k` → `ŋk`, the `ŋ` therefore remains the coda of one syllable and the `k` the
onset of the next, giving *seŋ.ka* rather than *seŋk.a*. To find the two
phonemes, smirjan splits the replacement into defined phonemes, trying the
longest first. Any other replacement, whether of one phoneme or of three or
more, becomes a single segment in the syllable of the pair's **first**
phoneme, and if either phoneme of the pair belonged to the nucleus, so does
the replacement.

After a replacement, the new segment is checked against its neighbours again,
so one replacement can create a pair that triggers another. A chain of
replacements that never settles makes the word fail instead of looping
forever.

Tables are applied **before** [filters](08-filters-and-rejects.md), so filters
see the result of the tables. They also apply where the two words of a solid
[compound](11-compounds.md#solid-compounds) meet.

## Tables and Filters Compared

Either mechanism can often do the same job, and the choice is mostly one of
clarity. A table is clearer when you're thinking in pairs, particularly when
many pairs are involved; a complete grid of nasals and stops, for instance,
takes one small table but a dozen filter rules. Filters are the better choice
for sequences longer than two phonemes, for anything that depends on the edges
of the word (`^`, `$`), and for rewrites that do not concern adjacent pairs.

Next: [Output Formats and Meaning Lists](10-output-and-meaning-lists.md)
