# 1. Getting started

## Building

smirjan needs JDK 27 and nothing else. With [SDKMAN!](https://sdkman.io), the
repository's `.sdkmanrc` selects the right JDK:

```sh
sdk env          # or: sdk use java 27.0.0-amzn
./build.sh       # builds build/smirjan.jar
./build.sh test  # builds and runs the self-tests
```

The `./smirjan` script in the repository root runs the jar.

## Running

smirjan takes two arguments: a definitions file and how many words to generate.

```sh
./smirjan mylang.def 25
```

Words go to standard output, one per line. Redirect them to save them:

```sh
./smirjan mylang.def 1000 > words.txt
```

Messages (warnings, errors, the seed notice below) go to standard error, so
they never end up in your word list.

## The first definition

A `.def` file is a list of `key: value` lines. This is about the smallest
useful one ([`examples/01-first.def`](examples/01-first.def)):

```
# The smallest useful definition: two classes and one syllable shape.
seed: first steps

C: p t k m n s
V: a i u

syllable: CV
```

- `C:` and `V:` define **classes**, named groups of phonemes. Any single
  uppercase letter can name a class. `C` and `V` are just conventional.
- `syllable: CV` says every syllable is a phoneme from `C` followed by one from `V`.
- `seed:` makes the output reproducible. More on that below.

```sh
$ ./smirjan docs/examples/01-first.def 10
kana
pama
maka
kupa
pata
pana
pusa
ti
tipi
pipa
```

Words have one, two or three syllables. With no length given, two syllables
are most common, then one, then three. [Chapter 5](05-word-length-and-weight.md)
covers word length.

Notice that `p` appears in seven of these ten words and `s` in only one. That's
deliberate: phonemes listed first are picked more often. [Chapter 2](02-phonemes-and-frequency.md)
explains how.

## File format basics

- One `key: value` per line. Spaces around the value are ignored.
- `#` starts a comment when it's at the start of a line or follows a space.
  `a#b` without spaces is ordinary text.
- Blank lines are ignored, except inside a [cluster table](09-cluster-tables.md), where a blank line ends it.
- Keys can appear in any order. The one exception is a class that includes
  another class: the included class must be defined first.
- Multi-letter keys are case-insensitive (`Syllable:` works). Class names are
  exactly one uppercase letter, `A` to `Z`.
- Unknown keys are errors, so typos are caught rather than silently ignored:

  ```
  smirjan: mylang.def: line 4: unknown key 'sylable'
  ```

- Files are read as UTF-8, so IPA and other scripts work directly. Text is
  normalised to Unicode NFC, so a precomposed `á` and `a` followed by a
  combining acute accent count as the same thing.

## Seeds

Everything random in smirjan comes from the seed. The same seed and the same
file give exactly the same words, in the same order, on any machine. A seed
can be any text:

```
seed: gobbledygook
```

Without a seed, smirjan picks a random one and tells you what it was:

```
smirjan: no seed given; using seed: <hex>
```

Copy that into your file to reproduce a run you liked.

The seed does more than pick phonemes. It also nudges every frequency slightly
(the *jitter*, see [chapter 2](02-phonemes-and-frequency.md#jitter)), and it
sets how often each optional element appears ([chapter 3](03-syllable-shapes.md#optional-elements)).
Two seeds therefore give two slightly different frequency profiles for the same
phonology, rather than just two different samples.

Next: [Phonemes, classes and frequency](02-phonemes-and-frequency.md)
