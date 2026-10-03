# 1. Getting Started

This chapter explains how to build and run smirjan, introduces the format of
a definition file by way of the smallest useful example, and describes the
seed, which governs every random choice the program makes.

## Building

smirjan requires JDK 25 and nothing else. With [SDKMAN!](https://sdkman.io),
the repository's `.sdkmanrc` selects the right JDK:

```sh
sdk env          # or: sdk use java 25.0.1-tem
./build.sh       # builds build/smirjan.jar
./build.sh test  # builds and runs the self-tests
```

The `./smirjan` script in the repository root runs the jar.

## Running

smirjan takes two arguments, a definition file and the number of words to
generate:

```sh
./smirjan mylang.def 25
```

The words are written to standard output, one per line, and can be kept by
redirecting the output to a file:

```sh
./smirjan mylang.def 1000 > words.txt
```

Everything else the program has to say, whether a warning, an error or the
seed notice described below, goes to standard error, so none of it ends up
in your word list.

## The First Definition

A `.def` file consists of lines of the form `key: value`. The following is
about the smallest definition that produces useful output
([`examples/01-first.def`](examples/01-first.def)):

```
# The smallest useful definition: two classes and one syllable shape.
seed: first steps

C: p t k m n s
V: a i u

syllable: CV
```

The lines `C:` and `V:` define two **classes**, that is, named groups of
phonemes. Any single uppercase letter can name a class; `C` for consonants
and `V` for vowels are a convention, and the program attaches no meaning to
the letters themselves. The line `syllable: CV` then states that every
syllable consists of one phoneme from `C` followed by one from `V`. The
`seed:` line makes the output reproducible, as explained under
[Seeds](#seeds) below.

```sh
$ ./smirjan docs/examples/01-first.def 10
tana
pama
maka
kupa
pata
pana
pusa
pi
tipi
pipa
```

When a definition says nothing about length, smirjan gives each attempt at a
word two syllables most often, then one, then three. The printed words follow
this order only in part. Because six consonants and three vowels allow only
eighteen words of one syllable, most one-syllable attempts repeat a word that
has already been produced, and since smirjan prints every word only once,
such repeats are discarded. Among the first 200 words of this example, 113
have two syllables, 70 three and only 17 one, and all eighteen possible
monosyllables have appeared by the thousandth word.
[Chapter 5](05-word-length-and-weight.md) shows how to set the lengths
explicitly.

The consonants are not evenly distributed. `p` occurs in eight of the ten
words and `s` in only one, because smirjan picks the phonemes listed first in
a class more often than those listed later, in proportions modelled on
natural languages. [Chapter 2](02-phonemes-and-frequency.md) explains how
those proportions are calculated.

## File Format Basics

A few general rules apply to every definition file.

Each line holds one `key: value` pair, and spaces around the value are
ignored. A `#` begins a comment when it stands at the start of a line or
after a space; written without a preceding space, as in `a#b`, it is ordinary
text. Blank lines are ignored everywhere except after a
[cluster table](09-cluster-tables.md), which they terminate.

Keys may appear in any order, except that a class which includes another
class must come after it. Multi-letter keys are case-insensitive, so
`Syllable:` is accepted, whereas a class name is always exactly one
uppercase letter from `A` to `Z`. A key that smirjan does not recognise is
treated as an error rather than skipped, which means that a misspelt key is
reported instead of silently having no effect:

```
smirjan: mylang.def: line 4: unknown key 'sylable'
```

Files are read as UTF-8, so IPA symbols and other scripts can be written
directly. The text is also normalised to Unicode NFC, which makes a
precomposed `á` and an `a` followed by a combining acute accent the same
phoneme, however the file happens to encode them.

## Seeds

Every random choice smirjan makes is derived from the seed, so the same file
with the same seed gives the same words, in the same order, on any machine.
A seed can be any text:

```
seed: gobbledygook
```

When a file gives no seed, smirjan picks one at random and reports it on
standard error:

```
smirjan: no seed given; using seed: <hex>
```

Copying that value into the file reproduces the run, which is worth doing
whenever a run produces words you'd like to keep generating.

The seed does more than choose phonemes. It also varies every frequency
slightly (the *jitter*, described in
[Chapter 2](02-phonemes-and-frequency.md#jitter)) and fixes how often each
optional element appears ([Chapter 3](03-syllable-shapes.md#optional-elements)).
Two different seeds therefore give the same phonology two slightly different
frequency profiles, rather than two samples drawn from one profile.

Next: [Phonemes, Classes and Frequency](02-phonemes-and-frequency.md)
