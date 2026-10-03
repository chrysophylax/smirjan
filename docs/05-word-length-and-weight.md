# 5. Word Length and Weight

Word length can be measured in two ways. The obvious measure is the number of
syllables, and this is what most definitions use. Many languages, however,
are sensitive to the weight of syllables rather than to their number. A
minimal word may have to contain two morae, for example, or a long vowel may
count for as much as two short ones. smirjan therefore allows length to be given in
syllables, in morae, or in both. The way it counts morae is set out in this
chapter, because [stress](06-stress.md) and [tone](07-tone-and-pitch.md)
depend on the same count.

## Length in Syllables

`word-syllables:` lists the possible word lengths, most common first, and
defaults to `2 1 3`. Ranges expand in order, and explicit weights work as
described in [chapter 2](02-phonemes-and-frequency.md#explicit-weights)
([`examples/05-length.def`](examples/05-length.def)):

```
C: t k n s m l r p
V: a i u e o

syllable: CV CVC
word-syllables: 1-2 3*0.5
```

Here `1-2 3*0.5` stands for lengths 1, 2 and 3 with weights 1 : 1 : 0.5. As
soon as one item in a list carries a weight, the whole list is read as
explicitly weighted, and an item without a weight counts as 1. Each attempt
at a word therefore draws a length of three syllables half as often as either
of the others, apart from the variation that jitter introduces.

The printed words do not reflect this ratio directly, because smirjan keeps
only distinct words. There are far fewer possible monosyllables than longer
words, so a monosyllabic attempt is more likely to repeat a word already
produced, and every repeat is discarded and replaced by a new attempt (see
[A Note on Duplicates](02-phonemes-and-frequency.md#a-note-on-duplicates)).
Monosyllables are thus under-represented in the output, and three-syllable
words over-represented, relative to the weights. Of the first 100 words of
this example, 27 have one syllable, 48 two and 25 three. The first twelve
are

```
tanu matunta pi kinisti nimata mak na ro se sakkak kukannol ne
```

## Morae

A **mora** is the unit in which syllable weight is measured. In a typical
analysis a short vowel contributes one mora, a long vowel two, and a coda
consonant one or none depending on the language; a syllable of two or more
morae is called **heavy**, one of a single mora **light**. smirjan counts the
morae of every syllable it generates and uses the count for three purposes.
It can hold every word to a fixed weight, as described in this section; it
can place stress on heavy syllables ([chapter 6](06-stress.md)); and it can
give each mora its own tone ([chapter 7](07-tone-and-pitch.md)).

### Counting

By default an onset contributes nothing to the weight of a syllable, while
each segment of the nucleus and each segment of the coda contributes one
mora. Under these defaults *ta* has one mora, *tan* two and *tans* three.
`morae:` changes the values for each position and can also assign a weight
to a particular class or phoneme:

```
morae: onset=0 nucleus=1 coda=1 A=2 aː=2
```

| item | meaning |
|---|---|
| `onset=` | weight of each onset segment (default 0) |
| `nucleus=` | weight of each nucleus segment (default 1) |
| `coda=` | weight of each coda segment (default 1); `coda=0` makes closed syllables light |
| `A=2` | segments of class `A` weigh 2 (e.g., long vowels) |
| `aː=2` | this phoneme weighs 2 |

When several values could apply to one segment, the most specific one is
used; a phoneme's own weight takes precedence over that of its class, and a
class weight takes precedence over the default for the position. Class and
phoneme weights apply only in the nucleus and the coda. An onset segment
always weighs the `onset=` value, in keeping with the usual observation
that onsets do not contribute to syllable weight.

### Words of a Fixed Weight

`word-morae:` requires every word to weigh exactly one of the listed amounts,
the most common first ([`examples/05-morae.def`](examples/05-morae.def)):

```
C: t k n s m l r p
V: a i u e o
A: aː iː uː

nucleus: V A
morae: onset=0 nucleus=1 coda=1 A=2

syllable: CV CVC CA
word-morae: 3 4
```

The weights of the generated words are easiest to inspect with `--tsv`
([chapter 10](10-output-and-meaning-lists.md)), which is also the quickest
way to check that a `morae:` line does what you meant:

| word | syllables | morae | weights |
|---|---|---:|---|
| sunani | su.na.ni | 3 | 1.1.1 |
| naːsa | naː.sa | 3 | 2.1 |
| tinuka | ti.nu.ka | 3 | 1.1.1 |
| taːna | taː.na | 3 | 2.1 |
| matil | ma.til | 3 | 1.2 |
| natti | nat.ti | 3 | 2.1 |
| kuliriti | ku.li.ri.ti | 4 | 1.1.1.1 |

The table shows how the same weight can be reached in different ways. Both
*sunani* and *natti* weigh three morae, the first as three light syllables
and the second as a heavy closed syllable followed by a light one; *matil*
has its heavy syllable at the end instead. In *naːsa* the long vowel of the
first syllable counts two morae, as the line `A=2` requires. Only *kuliriti*
weighs four. The proportion is the one the order of `word-morae: 3 4` leads
one to expect, since in a run of 1000 words 769 weigh three morae and 231
weigh four.

The two keys interact through the order in which smirjan builds a word. It
first chooses the target weight and the number of syllables, the latter from
`word-syllables:` if that key is present and otherwise at random between one
and the target weight; it then fills the syllables and discards the attempt
if their weight differs from the target. With both keys, a word therefore
satisfies both conditions at once. A combination that cannot be met, such as
two syllables of the shape `CV` with a target of four morae, never produces a
word.

Next: [Stress](06-stress.md)
