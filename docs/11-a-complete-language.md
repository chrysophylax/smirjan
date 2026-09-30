# 11. A complete language: Var Ysalenn

This chapter builds a definition for an existing conlang, Var Ysalenn, from its
grammar description ([`research/var-ysalenn-linguifex.pdf`](../research/var-ysalenn-linguifex.pdf)).
The finished file is [`examples/var-ysalenn.def`](../examples/var-ysalenn.def).

## What the description says

- **Consonants:** m n, p t d k g, t͡s d͡z, v ð s z x h, r, l.
- **Vowels:** i y u e o a, and the diphthongs *ea ou ai ei*.
- **Spelling:** mostly one letter per phoneme, but /x/ is ⟨ch⟩, /ð/ ⟨dd⟩,
  /t͡s/ ⟨ts⟩, /d͡z/ ⟨tz⟩, and a marginal long /nː/ is ⟨nn⟩.
- **Syllables:** mostly CVC, then CV, VC and V.
- **Onsets:** p t k d g v x h r l m n s. **Codas:** d g r l ð x s z t͡s d͡z and nasals.
- **Stress:** on the first syllable: [ʋaɾ ˈy.salɛnː].
- **Sample words:** *var*, *ysal* 'first one', *keiz kair* 'body', *Ysalenn*.

We'll write the words in the orthography, since that's how the language
presents them.

## Step 1: inventory and syllables

Phonemes are written as their spelling, so `ch`, `dd`, `ts`, `tz` and `nn` are
single phonemes. The description gives no frequencies, so the rank orders are
educated guesses.

```
seed: var ysalenn

C: t k d n l r m v p g h ch s     # onsets
V: a e i y o u ai ei ea ou        # vowels, then diphthongs
K: r l n s z d m dd ch g ts tz    # codas
E: K nn                           # codas, plus nn

syllable: CVK CV VK V
word-syllables: 2 1 3
```

```
irler voma lentuz disdei tyral milam tate nanve daini horvel ga inved enuch mu
```

It already sounds right, but vowel-initial syllables can follow open ones:
*keael*, *nedeaeim*, *nyear*. Worse, *e* + *a* across a syllable boundary is
spelled exactly like the diphthong *ea*.

## Step 2: shapes by position

Allowing vowel-initial syllables only at the start of a word removes the hiatus.
The same step reserves the marginal *nn* for the end of the word, as in
*Ysalenn*:

```
syllable-initial: CVK CV VK V
syllable-medial:  CVK CV
syllable-final:   CVE CV
syllable-mono:    CVE CV VE
```

```
irler vomar ditud disdeir lad temdair lal dir nimdyn nikeadkun neikus vedgel eidmus kagky
```

## Step 3: spelling-safe clusters

One problem remains. A coda *d* followed by an onset *d* is spelled ⟨dd⟩, which
reads as /ð/. Likewise *n* + *n* reads as long ⟨nn⟩, and *ch* + *h* doesn't
read as either. In 3000 words from step 2, 62 have such a sequence:

| word | phonemes | misread as |
|---|---|---|
| giddol | g i d d o l | /ð/ |
| nuddy | n u d d y | /ð/ |
| tanneir | t a n n ei r | /nː/ |

A [cluster table](09-cluster-tables.md) forbids exactly those pairs. It works
on phonemes, so it can tell the phoneme `dd` apart from `d` followed by `d`:

```
%   d  n  h
d   -  +  +
dd  -  +  +
n   +  -  +
nn  +  -  +
ch  +  +  -
```

After this, 5000 words contain no such sequence, while the digraph phonemes
themselves still appear in about one word in five.

## Step 4: stress

Stress falls on the first syllable, but the orthography doesn't write it. So
we compute it without marking it:

```
stress: initial
stress-mark: none
secondary-stress-mark: none
```

The printed words are unchanged, and `--tsv` records the stress:

| word | phonemes | syllables | stress |
|---|---|---|---:|
| irler | i r l e r | ir.ler | 1 |
| vomar | v o m a r | vo.mar | 1 |
| ditud | d i t u d | di.tud | 1 |
| disdeir | d i s d ei r | dis.deir | 1 |

The description mentions no tone, so there's no `tones:` line.

## Result

The sample words *var* and *kair* are among the words this definition
generates. A first vocabulary:

```
$ ./smirjan examples/var-ysalenn.def 10 --assign=lpj
nimdyn	to go
lad	rain
irler	house
lal	who?
ditud	3sg pronoun
disdeir	to bite
temdair	not
dir	knee
nikeadkun	skin/hide
vomar	to crush/grind
```

## Ideas for going further

- Word-final *l* can be [w] for some speakers: `filter: l$ > w` would generate
  that pronunciation.
- To avoid words that end in a diphthong followed by a cluster, add a reject
  such as `reject: (ai|ei|ea|ou){K}{K}`.
- Once you have a corpus, replace the guessed rank orders with real frequencies
  using [explicit weights](02-phonemes-and-frequency.md#explicit-weights).

Full reference: [Reference](reference.md)
