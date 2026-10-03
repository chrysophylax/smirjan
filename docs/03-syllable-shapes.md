# 3. Syllable Shapes

A syllable shape is a template for one syllable, written as a sequence of
class names and literal phonemes. This chapter describes what a shape may
contain, how elements are made optional, how different shapes can be
assigned to different positions in the word, and how smirjan divides each
syllable into onset, nucleus and coda.

## Several Shapes

`syllable:` takes a list of shapes rather than a single one, and the list is
ranked in the same way as the phonemes of a class, so **shapes listed first
are used more often** ([`examples/03-shapes.def`](examples/03-shapes.def)):

```
C: t k n s m l r p
V: a i u e o

syllable: CV CVC V
```

```
tikka tatis kaku nuti i tale ra taku mo ta kutat sa
```

Of the nineteen syllables in these twelve words, fifteen have the shape `CV`
and three the shape `CVC`; the bare-vowel shape `V`, ranked last, occurs only
once, as the word *i*. The proportions thus follow the order of the list.

A long list can be split over several `syllable:` lines, which are joined in
order as though they had been written on one line.

## What a Shape Can Contain

| element | meaning | example |
|---|---|---|
| `C` | one phoneme from class `C` | `CV` |
| a literal | exactly that phoneme | `sCV` → *sta*, *spa* |
| `X?` | `X` is optional | `CVC?` |
| `X?25` | `X` is optional, present 25 % of the time | `CVN?25` |
| `(…)` | an optional group | `(C)V(N)` |

To recognise literals, smirjan compares the shape against the phonemes of all
classes, trying the longest first, so `tʃa` is read as `tʃ` + `a` whenever
`tʃ` belongs to a class. Characters that match no phoneme are taken one at a
time, each together with any combining marks that follow it.

A shape cannot contain spaces, because spaces separate one shape from the
next. The only exception is inside square brackets, which are introduced in
[Chapter 4](04-restrictions.md).

## Optional Elements

A `?` after an element, or a pair of parentheses around a group of elements,
makes that element or group optional
([`examples/03-optional.def`](examples/03-optional.def)):

```
random-rate: 40%

C: t k n s m l r p
N: n m
V: a i u e o

syllable: CVN? (C)V CVC?10
```

```
iu sam nenaa naa ite tinin miku sau aa ni naane timruka
```

How often an optional element is present depends on how it is written. A `?`
or a group in parentheses without a number uses the `random-rate`, which
defaults to `30%` and is set to `40%` here. The rate is not applied
exactly, however. Each such element receives its own rate, varied around
`random-rate` by the seed, so that `N?` and `(C)` above are present at
slightly different rates. A number after the
`?`, as in `?10`, sets an exact percentage for that element, without any
variation; `?0` therefore never appears and `?100` always does. The
`random-rate` itself can be written as `40%`, `40` or `0.4`.

The output also contains vowel sequences such as *nenaa*, *sau* and *aa*.
They arise because the onset of `(C)V` is optional and the syllable can
follow any syllable that ends in a vowel. The next section shows one way of
preventing this; [filters and rejects](08-filters-and-rejects.md) and
[cluster tables](09-cluster-tables.md) offer others.

## Shapes by Position in the Word

In many languages syllables at the edges of a word are subject to different
constraints from those in the middle. smirjan models this with four keys,
each of which overrides `syllable:` for one position:

| key | used for |
|---|---|
| `syllable-initial:` | the first syllable of a word with two or more syllables |
| `syllable-medial:` | syllables between the first and the last |
| `syllable-final:` | the last syllable of a word with two or more syllables |
| `syllable-mono:` | a word with only one syllable |

A position without its own key falls back to `syllable:`, and when all four
are given, `syllable:` can be left out altogether
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
rikukun emin tanan nam tam kukaka titun uman kiton tatun resim kutan
```

Because a syllable without an onset is now possible only at the start of a
word, the vowel sequences of the previous example no longer occur. Most words
end in a nasal, since `CVN` is ranked first among the final shapes, and every
one-syllable word does, since `CVN` is the only shape allowed for them.

## Onset, Nucleus and Coda

smirjan determines which part of each syllable is the onset, which the
nucleus and which the coda. The division matters for
[morae](05-word-length-and-weight.md) and for the placement of
[stress and tone marks](06-stress.md).

The nucleus consists of the slots whose class is a **nucleus class**. By
default the only nucleus class is `V`, so if your vowels are spread over
several classes, all of them have to be listed:

```
V: a e i o u
A: aː eː iː oː uː
nucleus: V A
```

Everything before the first nucleus slot is then the onset, and everything
after the last one is the coda. A literal counts as part of the nucleus if
it is a member of a nucleus class. Syllabic consonants are handled in the
same way, by adding their class to `nucleus:`, e.g. `nucleus: V R` with
`R: r̩ l̩`.

Next: [Restrictions](04-restrictions.md)
