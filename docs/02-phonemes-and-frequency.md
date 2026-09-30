# 2. Phonemes, classes and frequency

## Classes

A class is a single uppercase letter followed by its phonemes, separated by
spaces:

```
D: n þ s t d
V: a i u e o
A: aː iː uː
```

A phoneme is any run of characters without spaces. It can be one letter,
several letters (`tʃ`, `aː`, `ng`, `ts`), or any script. The same phoneme
may appear in several classes.

Restrictions:

- A class name is exactly one letter from `A` to `Z`, and each class can only
  be defined once.
- A phoneme listed twice in one class is kept once, with a warning:

  ```
  smirjan: warning: line 1: 'p' appears twice in class C; keeping the first
  ```

- Syllable shapes read every uppercase letter as a class name, so an uppercase
  phoneme such as `N` can't be written directly in a shape (it can still
  belong to a class).
- `*` followed by a number sets a weight (see [below](#explicit-weights)), so a
  phoneme can't end in `*` plus digits.

## Including classes

A class can include classes defined **above** it by name. Their phonemes are
inserted in place, keeping their order
([`examples/02-inclusion.def`](examples/02-inclusion.def)):

```
P: p t k
N: m n
F: s h
C: P N F l      # p t k m n s h l, in that rank order
V: a i u e o

syllable: CV CVC
```

```
papi kuka tanan paspi panmi pu hiktana sa pusu paka
```

(From here on, sample output is shown on one line to save space. smirjan
itself always prints one word per line.)

## Rank order and distributions

Order within a class matters: **the first phoneme is picked most often, the
last least often.** How steeply frequency falls with rank is set by
`distribution`:

| `distribution:` | formula (rank *r* of *n*) | shape |
|---|---|---|
| `gusein-zade` (default) | F(r) = (ln(n+1) − ln r) / n | parameter-free, fits natural languages well |
| `yule` | F(r) = r<sup>−b</sup> · c<sup>r</sup> | flatter head, steeper tail; the best fit in Tambovtsev & Martindale's study of 95 languages |
| `zipf` | F(r) = 1 / r<sup>s</sup> | a very common first item, long tail |
| `flat` | F(r) = 1 | every phoneme equally likely |

The Gusein-Zade formula is from Borodovsky & Gusein-Zade (1989). See also
[the rank-frequency charts on Lachi-Lochu](https://lachi-lochu.conlang.org/conlang/gusein-zade.html).
The Yule formula as applied to phonemes is in
[`research/yule-distribution.pdf`](../research/yule-distribution.pdf).

Parameters:

| key | default | used by |
|---|---|---|
| `yule-b` | `0.5` | `yule` |
| `yule-c` | `0.9` | `yule` (must be positive; below 1 makes the tail fall faster) |
| `zipf-s` | `1` | `zipf` |

The distribution applies to **every** ranked list, not just phoneme classes.
Syllable shapes, word lengths and tones are ranked in the same way.

### Seeing the difference

This definition has six consonants and one vowel, and long words, so there are
plenty of consonants to count ([`examples/02-frequency.def`](examples/02-frequency.def)):

```
seed: frequency
distribution: gusein-zade

C: t n k s m p
V: a

syllable: CV
word-syllables: 50
```

Counting the consonants in 1000 words (50,000 phonemes) for each distribution:

| rank | phoneme | `gusein-zade` | `yule` | `zipf` | `flat` |
|---:|:---:|---:|---:|---:|---:|
| 1 | t | 38.7 % | 33.9 % | 41.5 % | 17.3 % |
| 2 | n | 25.9 % | 22.5 % | 21.5 % | 17.6 % |
| 3 | k | 14.9 % | 14.2 % | 12.3 % | 15.5 % |
| 4 | s | 10.6 % | 11.6 % | 9.7 % | 16.5 % |
| 5 | m | 7.0 % | 10.2 % | 8.5 % | 17.3 % |
| 6 | p | 2.8 % | 7.6 % | 6.5 % | 15.9 % |

The counts were made like this:

```sh
./smirjan docs/examples/02-frequency.def 1000 | grep -o '[tnksmp]' | sort | uniq -c
```

The raw Gusein-Zade values for six phonemes are 32.4, 20.9, 14.1, 9.3, 5.6
and 2.6 %. They sum to about 85 %, since the formula is an approximation, so
smirjan divides by the total. That gives 38.2, 24.6, 16.6, 11.0, 6.6 and 3.0 %.

## Jitter

Every weight is multiplied by a small seeded random factor, so that different
seeds give slightly different frequency profiles, as real languages have:

```
jitter: 10%     # the default: each weight varies by up to ±10 %
jitter: 0       # exact distribution values
```

With `jitter: 0` the example above gives 38.4, 24.7, 16.3, 10.8, 6.7, 3.0 %,
matching the normalised Gusein-Zade values. With the default jitter, changing
the seed from `frequency` to `other` gives 39.4, 21.8, 17.6, 11.8, 6.2, 3.2 %.
Each class, shape list and tone list has its own jitter stream, so adding a
class doesn't disturb the others.

Jitter can reorder two phonemes whose weights are close. Keep it small (it
must be below 100 %) if the exact order matters to you.

## Explicit weights

To set weights yourself, add `*weight` to items. The numbers are relative
([`examples/02-weights.def`](examples/02-weights.def)):

```
C: t*10 k*5 s*1
```

Here `t` is ten times as likely as `s`. As soon as any item in a list has a
weight, the whole list uses explicit weights, and items without one weigh `1`.
Jitter still applies. Measured over 50,000 consonants: `t` 59.6 %, `k` 33.6 %,
`s` 6.8 % (the exact ratios are 62.5, 31.25 and 6.25 %).

Explicit weights work in every ranked list: classes, syllable shapes, word
lengths and tones.

## A note on duplicates

smirjan never prints the same word twice. That has a side effect on small
phonologies: common words keep being generated and discarded, so the words
that *do* get printed contain rare phonemes more often than their weights
suggest.

With `word-syllables: 3`, `02-weights.def` can only make 3 × 3 × 3 = 27 words
at all, so asking for 30 prints those 27 and stops with exit status 3:

```
smirjan: only 27 distinct words could be generated from weights3.def
```

Frequencies behave as described when the number of possible words is much
larger than the number you ask for, which is the usual case for a real
phonology.

Next: [Syllable shapes](03-syllable-shapes.md)
