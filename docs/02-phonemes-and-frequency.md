# 2. Phonemes, Classes and Frequency

This chapter describes how phonemes are grouped into classes and how the
order of the phonemes within a class determines how often each is used. The
same ranking mechanism reappears in later chapters, applied to syllable
shapes, word lengths and tones, so it is worth understanding here in its
simplest form.

## Classes

A class is defined by a single uppercase letter followed by its phonemes,
separated by spaces:

```
D: n þ s t d
V: a i u e o
A: aː iː uː
```

A phoneme is any run of characters without a space in it. It may be a single
letter, a sequence of letters such as `tʃ`, `aː`, `ng` or `ts`, or characters
from any script, and the same phoneme may belong to several classes.

A few restrictions follow from the way the file is read. A class name is
exactly one letter from `A` to `Z`, and each class can be defined only once.
A phoneme listed twice in the same class is kept once, and smirjan warns
about the repetition:

```
smirjan: warning: line 1: 'p' appears twice in class C; keeping the first
```

Since syllable shapes read every uppercase letter as the name of a class, a
phoneme written in uppercase, such as `N`, cannot appear directly in a shape,
although it can still belong to a class. Finally, `*` followed by a number
sets a weight (see [Explicit Weights](#explicit-weights) below), so no
phoneme can end in `*` plus digits.

## Including Classes

A class can include any class defined **above** it by naming it among its
members. The phonemes of the included class are inserted at that point, in
their original order
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
papa kuka panan paspi patut pi hik pitup pamimik kita
```

Inclusion is a convenience for keeping natural classes such as stops and
nasals separate while still ranking them together. (From here on, sample
output is printed on one line to save space; smirjan itself always prints one
word per line.)

## Rank Order and Distributions

The order of the phonemes within a class is significant: **the first phoneme
is picked most often and the last least often.** Natural languages show the
same pattern, in that a few phonemes account for a large share of running
text while the rest are progressively rarer. How steeply frequency falls
from one rank to the next is set by the `distribution` key:

| `distribution:` | formula (rank *r* of *n*) | shape |
|---|---|---|
| `gusein-zade` (default) | F(r) = (1/r + 1/(r+1) + … + 1/n) / n | parameter-free, fits natural languages well |
| `yule` | F(r) = r<sup>−b</sup> · c<sup>r</sup> | flatter head, steeper tail; the best fit in Tambovtsev & Martindale's study of 95 languages |
| `zipf` | F(r) = 1 / r<sup>s</sup> | a very common first item, long tail |
| `flat` | F(r) = 1 | every phoneme equally likely |

The Gusein-Zade formula is explained in the next section. The application of
the Yule formula to phonemes is described in
[`research/yule-distribution.pdf`](../research/yule-distribution.pdf).

### The Gusein-Zade Model

The Gusein-Zade distribution comes from a model of letter frequencies that
Gusein-Zade (1988) proposed for Russian and that Borodovsky & Gusein-Zade
(1989) later applied to codons. The model rests on a single assumption. The
n frequencies of an inventory are positive and sum to one, so any particular
set of them is a point on a simplex; if nothing favours one set of
frequencies over another, that point is best taken to be drawn uniformly from
the simplex. The r-th largest frequency is then a random quantity, and the
model takes its expected value as the frequency of the phoneme of rank r.
Gusein-Zade shows that these expected values are the coordinates of the
centroid of the part of the simplex in which the frequencies already stand in
descending order, which gives

    F(r) = (1/n) (1/r + 1/(r+1) + … + 1/n).

For six phonemes this yields 40.8, 24.2, 15.8, 10.3, 6.1 and 2.8 %. The values
sum to exactly one, because the term 1/k occurs in the first k ranks and so
contributes k · 1/k, divided by n, to the total. They can therefore be used
as probabilities directly.

The paper goes on to approximate the sum by a logarithm,
F(r) ≈ (ln(n+1) − ln r) / n, and the formula usually circulates in this
form; the [rank-frequency charts on Lachi-Lochu](https://lachi-lochu.conlang.org/conlang/gusein-zade.html)
plot the approximation. smirjan does not use it, because it falls short of
one. The logarithm ln((n+1)/r) is the area under the curve 1/x between r and
n+1, and on each interval from k to k+1 that curve lies below 1/k, so every
rank receives a little less than its exact value. For six phonemes the
approximation gives 32.4, 20.9, 14.1, 9.3, 5.6 and 2.6 %, a total of 84.9 %,
and the shortfall diminishes only slowly as the inventory grows: the total is
92.8 % for twenty phonemes. That the exact values sum to one, and that every
approximated value and hence their total falls short, is proved for every
inventory size in [`proofs/GuseinZade.v`](../proofs/GuseinZade.v), which also
proves that the way smirjan turns weights into probabilities is sound.

The source is S. M. Gusein-Zade, "О распределении букв русского языка по
частоте встречаемости" [On the frequency distribution of letters in the
Russian language], *Problemy Peredachi Informatsii* 24(4), 1988, 102–107
([Math-Net.Ru](https://www.mathnet.ru/eng/ppi727)).

Two of the distributions take parameters:

| key | default | used by |
|---|---|---|
| `yule-b` | `0.5` | `yule` |
| `yule-c` | `0.9` | `yule` (must be positive; below 1 makes the tail fall faster) |
| `zipf-s` | `1` | `zipf` |

The chosen distribution applies to **every** ranked list in the file, and not
only to phoneme classes: syllable shapes, word lengths and tones are ranked
in the same way.

### Seeing the Difference

The effect of each distribution is easiest to see in a definition with six
consonants and a single vowel, generating long words so that there are many
consonants to count ([`examples/02-frequency.def`](examples/02-frequency.def)):

```
seed: frequency
distribution: gusein-zade

C: t n k s m p
V: a

syllable: CV
word-syllables: 50
```

Generating 1000 words (50,000 phonemes) under each distribution in turn and
counting the consonants gives the following shares:

| rank | phoneme | `gusein-zade` | `yule` | `zipf` | `flat` |
|---:|:---:|---:|---:|---:|---:|
| 1 | t | 41.4 % | 33.9 % | 41.5 % | 17.3 % |
| 2 | n | 25.4 % | 22.5 % | 21.5 % | 17.6 % |
| 3 | k | 14.2 % | 14.2 % | 12.3 % | 15.5 % |
| 4 | s | 10.0 % | 11.6 % | 9.7 % | 16.5 % |
| 5 | m | 6.5 % | 10.2 % | 8.5 % | 17.3 % |
| 6 | p | 2.6 % | 7.6 % | 6.5 % | 15.9 % |

The counts were made with:

```sh
./smirjan docs/examples/02-frequency.def 1000 | grep -o '[tnksmp]' | sort | uniq -c
```

Under `gusein-zade` the measured shares lie close to the model's 40.8, 24.2,
15.8, 10.3, 6.1 and 2.8 %, and what difference remains is the jitter
described below. Zipf with its default exponent happens to give the first of
six items almost the same share (1 / (1 + ½ + … + ⅙) ≈ 40.8 %), but its tail
is far longer: `p` keeps 6.5 % under Zipf and only 2.6 % under Gusein-Zade.
Yule keeps the rarest consonants comparatively common, and under `flat` the
remaining differences come mainly from jitter (with `jitter: 0` the six
shares lie between 16.3 and 16.9 %).

## Jitter

Before it is used, every weight is multiplied by a small random factor drawn
from the seed. The purpose is to give each seed a slightly different
frequency profile, as real languages have, instead of reproducing the formula
exactly every time:

```
jitter: 10%     # the default: each weight varies by up to ±10 %
jitter: 0       # exact distribution values
```

The factor is drawn uniformly between 1 − j and 1 + j, where j is the jitter,
so that with the default each weight may end up anywhere between 90 and 110 %
of its formula value. With `jitter: 0` the example above gives 41.0, 24.2,
15.6, 10.2, 6.2 and 2.8 %, and the small differences from the model's values
are the sampling error of 50,000 draws. With the default jitter and the seed
changed from `frequency` to `other`, the same definition gives 42.1, 21.5,
16.8, 11.0, 5.8 and 2.9 %: the order of the phonemes is unchanged, while
individual shares move by up to three percentage points. Each class, shape
list and tone list draws its jitter from a separate random stream, so adding
a class to a definition leaves the frequencies of the existing ones as they
were.

Two weights can change places only if the smaller is more than
(1 − j) / (1 + j) of the larger, which is 0.82 at the default of 10 %. Under
the Gusein-Zade model no two neighbouring ranks are that close in a class of
up to thirteen phonemes, so the order in which such a class is written is
always preserved. In larger classes the weights of neighbouring ranks draw
closer together, and some seeds swap them. In a class of fourteen this can
happen anywhere between ranks 4 and 8, and in a class of thirty between
ranks 2 and 25. A smaller jitter prevents it if the exact order matters to
you; the value must in any case be below 100 %.

## Explicit Weights

When the frequencies should not follow a formula at all, weights can be set
by hand by adding `*weight` to the items of a list. The numbers are relative
to one another ([`examples/02-weights.def`](examples/02-weights.def)):

```
C: t*10 k*5 s*1
```

Here `t` is ten times as likely as `s`. As soon as any item in a list has a
weight, the whole list uses explicit weights, and items written without one
weigh `1`. Jitter applies to explicit weights as well. If the example is
changed to words of fifty syllables and 1000 words are generated, the 50,000
consonants divide into 59.6 % `t`, 33.6 % `k` and 6.8 % `s`, where the exact
ratios would give 62.5, 31.25 and 6.25 %.

Explicit weights are accepted in every ranked list: classes, syllable
shapes, word lengths and tones.

## A Note on Duplicates

smirjan never prints the same word twice, and on small phonologies this
affects the frequencies. The commonest words keep being generated and then
discarded as duplicates, so the words that *are* printed contain rare
phonemes more often than their weights alone would suggest.

`02-weights.def` shows the limit of this. With `word-syllables: 3` it can
form only 3 × 3 × 3 = 27 words at all, so a request for 30 prints those 27
and then stops with exit status 3:

```
smirjan: only 27 distinct words could be generated from weights3.def
```

The frequencies described in this chapter hold when the number of possible
words is much larger than the number requested, which is the usual situation
with the phonology of a real language.

Next: [Syllable Shapes](03-syllable-shapes.md)
