# 3. Syllable shapes

## Several shapes

`syllable:` takes a list of shapes. Like phonemes in a class, **shapes listed
first are used more often** ([`examples/03-shapes.def`](examples/03-shapes.def)):

```
C: t k n s m l r p
V: a i u e o

syllable: CV CVC V
```

```
tikka tatim kaku nuti i tale ra tanu mo ta kutit sa
```

Long lists can be split over several `syllable:` lines. They're joined in
order, as if written on one line.

## What a shape can contain

| element | meaning | example |
|---|---|---|
| `C` | one phoneme from class `C` | `CV` |
| a literal | exactly that phoneme | `sCV` → *sta*, *spa* |
| `X?` | `X` is optional | `CVC?` |
| `X?25` | `X` is optional, present 25 % of the time | `CVN?25` |
| `(…)` | an optional group | `(C)V(N)` |

Literals are matched against the phonemes of all your classes, longest first,
so `tʃa` is read as `tʃ` + `a` if `tʃ` is in a class. Anything else is taken
one character at a time, with any combining marks attached.

A shape can't contain spaces, since spaces separate shapes (except inside
`[…]`, see [chapter 4](04-restrictions.md)).

## Optional elements

`?` after an element, or parentheses around a group, make it optional
([`examples/03-optional.def`](examples/03-optional.def)):

```
random-rate: 40%

C: t k n s m l r p
N: n m
V: a i u e o

syllable: CVN? (C)V CVC?10
```

```
ue mam nenaa naa ite tinin miku sau aa si niane timruka
```

- `?` and `(…)` without a number use `random-rate` (default `30%`). Each
  optional element gets its **own** seeded variation of that rate, so `N?` and
  `(C)` above are present at slightly different rates, fixed by the seed.
- `?10` sets an exact percentage for that element, with no variation. `?0`
  never appears; `?100` always does.
- `random-rate` accepts `40%`, `40` or `0.4`.

The output above has vowel sequences such as *nenaa*, *sau*, *aa*, because
`(C)V` can follow any open syllable. The next section shows one way to prevent
that; [filters and rejects](08-filters-and-rejects.md) and
[cluster tables](09-cluster-tables.md) are others.

## Shapes by position in the word

Syllables often behave differently at the edges of a word. These keys override
`syllable:` for one position:

| key | used for |
|---|---|
| `syllable-initial:` | the first syllable of a word with two or more syllables |
| `syllable-medial:` | syllables between the first and the last |
| `syllable-final:` | the last syllable of a word with two or more syllables |
| `syllable-mono:` | a word with only one syllable |

Any position you don't specify falls back to `syllable:`. If all four are
given, `syllable:` can be left out
([`examples/03-positions.def`](examples/03-positions.def)):

```
C: t k n s m l r p
N: n m
V: a i u e o

syllable-initial: CV V
syllable-medial:  CV
syllable-final:   CVN CV
syllable-mono:    CVN
```

```
rikukun emin tanan nam tam kukaka titun uman kiton katun resim nutan
```

Vowel-initial syllables now only start words, so there's no hiatus. Words
usually end in a nasal, and one-syllable words always do.

## Onset, nucleus and coda

smirjan works out which part of each syllable is the onset, the nucleus and
the coda. This matters for [morae](05-word-length-and-weight.md) and for where
[stress and tone marks](06-stress.md) go.

The nucleus is made of the slots whose class is a **nucleus class**. By
default that's `V`. If your vowels live in other classes, list them all:

```
V: a e i o u
A: aː eː iː oː uː
nucleus: V A
```

Everything before the first nucleus slot is the onset, everything after the
last one is the coda. A literal counts as nuclear if it's a member of a
nucleus class. For syllabic consonants, add their class to `nucleus:`
(e.g. `nucleus: V R` with `R: r̩ l̩`).

Next: [Restrictions](04-restrictions.md)
