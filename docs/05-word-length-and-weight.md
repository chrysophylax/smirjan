# 5. Word length and weight

## Length in syllables

`word-syllables:` lists possible word lengths, most common first. The default
is `2 1 3`. Ranges expand in order, and explicit weights work as in
[chapter 2](02-phonemes-and-frequency.md#explicit-weights)
([`examples/05-length.def`](examples/05-length.def)):

```
C: t k n s m l r p
V: a i u e o

syllable: CV CVC
word-syllables: 1-2 3*0.5
```

`1-2 3*0.5` means lengths 1, 2 and 3, weighted 1 : 1 : 0.5. Because one item
has a weight, the list uses explicit weights and the range items weigh 1 each.

```
tanu matunta pi kiniski nimata mak nat patla kimkiti kis tinolti kaktisa
```

## Morae

A mora is a unit of syllable weight. smirjan counts the morae of every
syllable, which lets you:

- make every word weigh a set amount (this section),
- stress heavy syllables ([chapter 6](06-stress.md)),
- put a tone on each mora ([chapter 7](07-tone-and-pitch.md)).

### Counting

By default an onset weighs nothing, and each nucleus segment and each coda
segment weighs one mora. So *ta* has 1 mora, *tan* 2, *tans* 3. `morae:`
changes these values, and can set the weight of particular classes or
phonemes:

```
morae: onset=0 nucleus=1 coda=1 A=2 aː=2
```

| item | meaning |
|---|---|
| `onset=` | weight of each onset segment (default 0) |
| `nucleus=` | weight of each nucleus segment (default 1) |
| `coda=` | weight of each coda segment (default 1). `coda=0` gives a language where closed syllables are light |
| `A=2` | segments from class `A` weigh 2 (e.g. long vowels) |
| `aː=2` | this phoneme weighs 2 |

A phoneme's own weight wins over its class's, which wins over the position
default. Class and phoneme weights only apply in the nucleus and coda. Onsets
always weigh the `onset=` value.

### Words of a fixed weight

`word-morae:` requires every word to weigh exactly one of the listed amounts,
most common first ([`examples/05-morae.def`](examples/05-morae.def)):

```
C: t k n s m l r p
V: a i u e o
A: aː iː uː

nucleus: V A
morae: onset=0 nucleus=1 coda=1 A=2

syllable: CV CVC CA
word-morae: 3 4
```

With `--tsv` ([chapter 10](10-output-and-meaning-lists.md)) you can see the
weights:

| word | syllables | morae | weights |
|---|---|---:|---|
| sunani | su.na.ni | 3 | 1.1.1 |
| kattiː | kat.tiː | 4 | 2.2 |
| kisti | kis.ti | 3 | 2.1 |
| kuksi | kuk.si | 3 | 2.1 |
| turama | tu.ra.ma | 3 | 1.1.1 |
| sasititu | sa.si.ti.tu | 4 | 1.1.1.1 |

With only `word-morae:`, the number of syllables is whatever adds up to the
target. With both `word-syllables:` and `word-morae:`, a word must satisfy both.

Next: [Stress](06-stress.md)
