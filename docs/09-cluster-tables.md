# 9. Cluster tables

A cluster table decides, for pairs of adjacent phonemes, whether the pair is
allowed, forbidden, or replaced by something else. It's a compact way to write
many rules about consonant clusters, vowel sequences or assimilation at once.

The idea and the notation come from William Annis's
[Lexifer](https://lingweenie.org/conlang/lexifer/). smirjan's tables follow
Lexifer's.

## Notation

```
% a  i  u
a +  +  o
i -  +  uu
u -  -  +
```

- A table starts with a line beginning with `%`. The rest of that line lists
  the **second** phoneme of each pair.
- Each following line starts with the **first** phoneme of a pair, then has one
  cell per column.
- A blank line ends the table. So does a new `key: value` line or another `%`
  line. Comment lines don't end it.

Each cell is one of:

| cell | meaning |
|---|---|
| `+` | the pair is allowed |
| `-` | the pair is forbidden; any word containing it is discarded |
| anything else | the pair is replaced by this text |

So in the table above, *ai* is fine, *au* becomes *o*, *ia* is forbidden and
*iu* becomes *uu*.

Pairs that no table mentions are allowed. A file can have several tables; if
two tables cover the same pair, the later one wins.

## What counts as a pair

Tables check **every pair of adjacent phonemes in the word**, including pairs
that straddle a syllable boundary. That's usually what matters for clusters:
the coda of one syllable meeting the onset of the next.

Pairs are phonemes, not characters. If `tʃ` is a phoneme, the table sees
`tʃ` + `a`, never `ʃ` + `a`.

Row and column labels can be class names, which expand to every phoneme in the
class:

```
% T
N -        # no nasal directly before a member of T
```

## Example: nasal assimilation

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

Read the rows as: *m* before *t* becomes *nt*, before *k* becomes *ŋk*, and
can't come before *s* or *l*; *n* before *p* becomes *mp*, before *k* becomes
*ŋk*, and before *l* becomes *ll*.

Counting clusters in 2000 words with and without the table:

| | mp | mt | mk | ms | ml | np | nt | nk | ns | nl | ŋk | ll |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| without | 140 | 198 | 133 | 78 | 56 | 44 | 97 | 41 | 30 | 21 | 0 | 0 |
| with | 190 | 0 | 0 | 0 | 0 | 0 | 287 | 0 | 31 | 0 | 177 | 20 |

Nasals now always agree with a following stop:

```
saŋku santampi kempi piŋkatim pusimpa lintuŋkam
```

## Substitutions in detail

- The replacement becomes a single segment in the syllable of the pair's
  **first** phoneme.
- If either phoneme of the pair was in the nucleus, the replacement is too.
- After a replacement, the new segment is checked against its neighbours again,
  so replacements can feed further rules. A chain that never settles makes the
  word fail rather than loop forever.
- Tables are applied **before** [filters](08-filters-and-rejects.md), so
  filters see the result of the tables.

## Tables or filters?

Both can do the same job. A table is clearer when you're thinking in pairs,
especially many pairs at once (a full nasal-plus-stop grid is one small table
but a dozen filter rules). Filters are better for anything longer than two
phonemes, anything that depends on word edges (`^`, `$`), and for rewrites
that aren't about adjacent pairs.

Next: [Output formats and meaning lists](10-output-and-meaning-lists.md)
