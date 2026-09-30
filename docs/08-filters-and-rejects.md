# 8. Filters and rejects

Shapes and exceptions work one syllable at a time. Filters and rejects look at
the **whole word**, so they can handle sound changes, spelling rules and
restrictions across syllable boundaries.

Both use [Java regular expressions](https://docs.oracle.com/en/java/javase/27/docs/api/java.base/java/util/regex/Pattern.html),
plus one addition: **`{C}` means "any phoneme of class `C`"**.

## Filters

A filter rewrites every match of a pattern:

```
filter: ti > tʃi
```

- Several rules can go on one line, separated by `;`, and `filter:` can be
  repeated. Rules apply in the order written, each to the output of the previous one.
- `!` or an empty right-hand side deletes: `filter: h$ > !`
- Groups can be reused: `filter: ([aeiou])h > $1ː`

## Rejects

A reject discards any word that matches one of its patterns:

```
reject: ^{C}u u$ ll.*ll
```

Patterns are separated by spaces, so a pattern can't contain a space. Words
never contain spaces anyway. `reject:` can be repeated.

## Example

([`examples/08-filters.def`](examples/08-filters.def))

```
C: t k s n m l p
V: a i u e o

syllable: CV CVC
word-syllables: 2 3

filter: ti > tʃi; si > ʃi; {C}l > ll
reject: ^{C}u u$ ll.*ll
```

| | output |
|---|---|
| without filters | kamtak nasi tekita saptat kala taknat kaso lati tane kani nitani kitip |
| with filters | kamtak naʃi tekita saptat kala taknat kaso latʃi tane kani nitani kitʃip |

Over 2000 words:

| | *ti* | *si* | *tʃi* | Cu- | -u | consonant + *l* | *ll* |
|---|---:|---:|---:|---:|---:|---:|---:|
| without | 386 | 209 | 0 | 303 | 239 | 38 | 2 |
| with | 0 | 0 | 408 | 0 | 0 | 0 | 42 |

Every *ti* and *si* was palatalised, and every consonant + *l* cluster became
*ll* (*tellak*, *kalla*, *nalla*). No word starts with a consonant + *u*,
ends in *u*, or has two *ll*.

## Order of operations

1. Syllables are generated from their shapes.
2. [Cluster tables](09-cluster-tables.md) are applied.
3. Filters are applied, in order.
4. Rejects are checked.
5. The [mora target](05-word-length-and-weight.md#words-of-a-fixed-weight) is checked.
6. [Stress](06-stress.md) and [tone](07-tone-and-pitch.md) are assigned and written.

Because marks are added last, patterns only ever see plain phonemes, never
`ˈ`, accents or syllable separators. `^` and `$` match the start and end of the
word.

Filter output keeps its syllable. Replaced text belongs to the syllable where
the match started, and it's split back into the phonemes you defined. So in
*latʃi*, `tʃ` is the onset and `i` the nucleus. Stress and tone still land on
the vowel, and weights still count correctly. Characters that aren't defined
as phonemes (like `ʃ` here, when only `t` is defined) attach to the phoneme
before them.

Next: [Cluster tables](09-cluster-tables.md)
