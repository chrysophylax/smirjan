# 12. A Complete Language: Var Ysalenn

The preceding chapters introduced the parts of a definition one at a time.
This chapter puts them together in a definition for an existing constructed
language, Var Ysalenn, working from its grammar description
([`research/var-ysalenn-linguifex.pdf`](../research/var-ysalenn-linguifex.pdf)).
Each step addresses a problem that shows up in the output of the step before,
which is how a definition is usually developed in practice. The finished file
is [`examples/var-ysalenn.def`](../examples/var-ysalenn.def).

## What the Description Says

The description supplies the following facts, and the definition is built from
nothing else:

- **Consonants:** m n, p t d k g, t͡s d͡z, v ð s z x h, r, l.
- **Vowels:** i y u e o a, and the diphthongs *ea ou ai ei*.
- **Spelling:** mostly one letter per phoneme, but /x/ is ⟨ch⟩, /ð/ ⟨dd⟩,
  /t͡s/ ⟨ts⟩, /d͡z/ ⟨tz⟩, and a marginal long /nː/ is ⟨nn⟩.
- **Syllables:** mostly CVC, then CV, VC and V.
- **Onsets:** p t k d g v x h r l m n s. **Codas:** d g r l ð x s z t͡s d͡z and nasals.
- **Stress:** on the first syllable: [ʋaɾ ˈy.salɛnː].
- **Sample words:** *var*, *ysal* 'first one', *keiz kair* 'body', *Ysalenn*.

The words are generated in the orthography rather than in IPA, since that is
the form in which the description presents the language.

## Step 1: Inventory and Syllables

Each phoneme is written as it is spelled, so the digraphs `ch`, `dd`, `ts`,
`tz` and `nn` are single phonemes as far as smirjan is concerned. The
description gives no frequencies, and the rank orders below are therefore
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
irler vora lentoz disdei tirar milam tate nanva dunin okul neikus vedgel eizmu kelo
```

The result already has much of the character of the language, but nothing yet
prevents a vowel-initial syllable from following an open one, which produces
vowel sequences such as those in *kadual* (ka.du.al), *tios* (ti.os) and *laur*
(la.ur). Some of these are merely unattested. Others are misleading, because
*e* followed by *a* across a syllable boundary is spelled exactly like the
diphthong *ea*: *teal* (te.al), which the same definition produces within its
first 3000 words, cannot be told apart from a one-syllable word with *ea*.

## Step 2: Shapes by Position

Syllable shapes can be restricted by their position in the word (see
[Shapes by Position in the Word](03-syllable-shapes.md#shapes-by-position-in-the-word)).
If vowel-initial syllables are allowed only at the beginning of a word, every
vowel inside a word is preceded by a consonant and the hiatus disappears. The
same step confines the marginal long *nn* to the end of the word, where it
occurs in *Ysalenn*:

```
syllable-initial: CVK CV VK V
syllable-medial:  CVK CV
syllable-final:   CVE CV
syllable-mono:    CVE CV VE
```

```
irler vorar detoz disdeir nad teddair lar dir nimdyn nin okun neikus vedgel eizmus
```

## Step 3: Spelling-Safe Clusters

The remaining problem is again one of spelling. A coda *d* followed by an onset
*d* is written ⟨dd⟩, which a reader takes for /ð/; in the same way, *n*
followed by *n* reads as the long ⟨nn⟩, and *ch* followed by *h* cannot be
read correctly at all. Of 3000 words generated with the definition from step 2,
60 contain such a sequence, among them *teddair* in the sample above:

| word | phonemes | misread as |
|---|---|---|
| teddair | t e d d ai r | /ð/ |
| uddir | u d d i r | /ð/ |
| lynnad | l y n n a d | /nː/ |

A [cluster table](09-cluster-tables.md) forbids exactly these sequences. Each
row of the table is the last phoneme of a syllable and each column the first
phoneme of the next, and a minus sign forbids the pair. Because the table
operates on phonemes rather than letters, it can tell the phoneme `dd` apart
from a `d` followed by another `d`:

```
%   d  n  h
d   -  +  +
dd  -  +  +
n   +  -  +
nn  +  -  +
ch  +  +  -
```

With the table in place, 5000 generated words contain no such sequence, while
the digraph phonemes themselves still occur in 873 of them, a little over one
word in six. The ambiguity is removed without removing the sounds; *teddair*,
for instance, no longer appears in the sample.

## Step 4: Stress

Var Ysalenn stresses the first syllable, but its orthography does not mark
stress. The definition therefore has stress computed but not printed:

```
stress: initial
stress-mark: none
secondary-stress-mark: none
```

The printed words are the same as after step 3, and `--tsv` records the
position of the stress in each of them:

| word | phonemes | syllables | stress |
|---|---|---|---:|
| irler | i r l e r | ir.ler | 1 |
| vorar | v o r a r | vo.rar | 1 |
| detoz | d e t o z | de.toz | 1 |
| disdeir | d i s d ei r | dis.deir | 1 |

The description does not mention tone, so the definition has no `tones:` line.

## Step 5: Compounds

According to the description, Var Ysalenn has "a large corpus of samāhāra
dvandva-type compounds", written as two words, such as *keiz kair* 'body',
literally 'bones bloods'. The definition accordingly enables
[compounds](11-compounds.md), makes most of them dvandvas, and writes them with
a space. The description does not say in which order the parts of a
determinative compound stand. Its models are Standard Average European
languages, which put the head last, so the definition makes head-final the
main order and leaves a little room for the other:

```
compounds: yes
compound-rate: 40%
compound-types: dvandva*3 determinative
compound-order: head-final*95 head-first*5
compound-separator: space
```

With a meaning list, compounds are formed from the words for related meanings.
Run with the full WOLD list (`… 1460 --assign=wlt`), the definition expresses
145 of the 1460 meanings as compounds. Eight of them, selected from that output
to show different kinds of relation between the parts, are:

```
yla kairea	the bolt of lightning	= the thunder + the lightning
lem um	the bull	= the animal + the livestock
aildu red	the sow	= the boar + the pig
nidan eskyn	the breakfast	= the bread + the meal
kydous emve	the mill	= the pestle + the mortar(1)
itin dekas	the mead	= the wine + the beer
veker talga	the morning	= the dawn + the east
elmai tysteinval	the older sibling	= the older brother + the older sister
```

Each part is also a word in its own right, with an entry of its own in the
lexicon: *lem* 'animal', *um* 'livestock', *aildu* 'boar' and *veker* 'dawn'
all appear as simple words. *elmai tysteinval* 'older sibling' shows the
treatment of kinship terms described in
[Where the Related Meanings Come From](11-compounds.md#where-the-related-meanings-come-from).
Its parts are linked to it by explicit relations in the data, whereas
colexification plays no part in kinship terms.

## Result

The sample words *var* and *kair* from the description are among the words
this definition generates (in a run of 20,000 words, as the 649th and the
3087th). A first basic vocabulary follows. The Leipzig-Jakarta list consists
almost entirely of meanings that languages express with simple words, and in
a run of all hundred meanings only two, 'neck' and 'thigh', are compounds.
The first ten lines of that run, which give the ten highest-ranked meanings
of the list, are:

```
$ ./smirjan examples/var-ysalenn.def 100 --assign=lpj | head
okun	fire
teikeizta	nose
irler	to go
nad	water
on	mouth
enad	tongue
te	blood
malnodrar	bone
vorar	2sg pronoun
ta	root
```

Most of these basic meanings have short words, such as *on* 'mouth' and *te*
'blood'. The random element in the pairing (see
[chapter 10](10-output-and-meaning-lists.md#--assign-meaning-lists)) has
nevertheless given 'nose' a word of seven phonemes, *teikeizta*, and 'bone'
one of the two longest words in the run, *malnodrar*, with nine. Over the
whole run, however, the simple words for the ten most basic meanings
have 4.3 phonemes on average, and those for the ten least basic 5.7.

## Ideas for Going Further

The definition can be refined in several directions. Some speakers pronounce
word-final *l* as [w], and `filter: l$ > w` generates that pronunciation. A
reject such as `reject: (ai|ei|ea|ou){K}{K}` excludes words in which a
diphthong is followed by two coda consonants, should such words turn out to be
foreign to the language. Once you have a corpus of Var Ysalenn text, the
guessed rank orders can give way to observed frequencies through
[explicit weights](02-phonemes-and-frequency.md#explicit-weights).

Full reference: [Reference](reference.md)
