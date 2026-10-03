# 7. Tone and Pitch Accent

Tone is optional, and a definition without a `tones:` key produces words
without it. In a **tone language** the pitch of a syllable distinguishes
words in the same way as its consonants and vowels; in Yoruba, for example,
words can differ in tone alone. smirjan can assign a tone to every syllable, or to
every mora where a language treats a long vowel as carrying two tones, or to
the stressed syllable only, which yields a pitch-accent system. This chapter
treats these three arrangements in turn, together with the question of where
in the written word the tone appears.

## Tones per Syllable

`tones:` lists the tones of the language, most common first. Many tonal
orthographies leave one tone unwritten, typically the mid or the low, and
`-` represents such a tone in the list
([`examples/07-tone.def`](examples/07-tone.def)):

```
C: t k n s m l p
N: n ŋ
V: a i u e o

syllable: CV CVN
word-syllables: 1 2

tones: - acute grave
```

```
tamo sa má ma tan ti tu kí tin na katu kú
```

Because the unwritten tone comes first in the list, it is also the most
frequent, and most syllables in the output carry no mark. The acute appears
in *má*, *kí* and *kú*. The grave, ranked last, is absent from these twelve
words, although it occurs in 52 of the first 200 (*tà*, *tùn*, *tì*), so its
absence here reflects its lower frequency and the size of the sample.

A tone can be written in four ways. It can be the name of a diacritic, from
the same set of names that serves for
[stress marks](06-stress.md#how-stress-is-written) (`acute`, `grave`,
`macron`, `circumflex`, `caron` and so on); it can be the combining
character itself, typed directly; it can be a sequence of tone letters such
as `˥ ˧ ˩` or `˧˥`; or it can be any other text, such as the numbers `1 2 3`
or the letters `H L` used in many descriptive grammars.

## Where the Tone Is Written

`tone-position:` determines where the tone is placed relative to the
syllable:

| value | placement |
|---|---|
| `nucleus` | as a diacritic on the syllable's vowel (default when every tone is a diacritic) |
| `after` | after the syllable (default otherwise) |
| `before` | before the syllable |

The defaults follow from the form of the tones. A diacritic belongs on a
vowel, whereas a tone letter or a number cannot be attached to one and is
written beside the syllable instead. Chao tone letters, the usual IPA
notation in descriptions of East and Southeast Asian languages, are
therefore written after each syllable without further configuration
([`examples/07-tone-letters.def`](examples/07-tone-letters.def)):

```
syllable: CV
word-syllables: 1 2

tones: ˥ ˧ ˩ ˧˥ ˥˩
```

```
ke˥ su˥ ke˧ ki˥ta˧ ki˥ ka˥ka˧ sa˧ ku˧te˥ ta˥˩ ka˥
```

The list contains three level tones (high, mid and low) and two contours, a
rise `˧˥` and a fall `˥˩`. As with any list, the earlier tones are the more
frequent, so the high and mid tones dominate the sample.

## Tone per Mora

`tone-bearing:` determines which units carry a tone:

| value | meaning |
|---|---|
| `syllable` (default) | one tone per syllable |
| `mora` | one tone per mora of the nucleus |
| `stressed` | only the stressed syllable ([pitch accent](#pitch-accent)) |

In some languages a long vowel behaves as two tone-bearing units, so that a
fall on a long vowel is analysed as a high tone followed by a low one.
`tone-bearing: mora` models this directly by giving a long vowel of two morae
two tones. A **contour tone**, a pitch movement within a
single vowel, then arises from the combination of the two, and
`tone-contour:` specifies how each combination is written
([`examples/07-mora-tone.def`](examples/07-mora-tone.def)):

```
V: a i u
A: aː iː uː

nucleus: V A
morae: A=2

syllable: CV CA
word-syllables: 1 2

tones: acute grave
tone-bearing: mora
tone-contour: acute+grave=circumflex grave+acute=caron
```

```
nǐː níkà ní ká kà pá kǎː tásí síká táː
```

In *nǐː* the long vowel carries a low tone followed by a high one, and the
entry `grave+acute=caron` writes that rise with a caron. In *táː* both morae
carry the high tone, and a level tone is written only once. A combination
for which no `tone-contour:` entry exists is written with both marks stacked
on the vowel. A different rule applies when the nucleus is spelled with as
many letters as it has tones, as in `aa` or `ai`: each letter then carries
its own tone, giving forms such as *páà*, and no contour is needed.

Only the morae of the nucleus carry tone. A coda consonant may add a mora to
the weight of a syllable, and that mora counts for stress, but it never
receives a tone of its own.

## Pitch Accent

In a **pitch-accent** system, such as that of Japanese, a word has at most
one prominent syllable, and the prominence is realised as a tone rather than
as a stress. With `tone-bearing: stressed`, smirjan gives a tone to the
stressed syllable only and leaves the others unmarked. Combined with
`stress-mark: none`, which suppresses the stress mark, the tone becomes the
only written sign of the accent
([`examples/07-pitch-accent.def`](examples/07-pitch-accent.def)):

```
syllable: CV CVC
word-syllables: 2 3

stress: free
stress-mark: none
tones: acute circumflex
tone-bearing: stressed
```

```
tíka kapâka tutá karamî nimsikát natmónti tukíni titú tisî takú
```

Since the stress rule is `free`, the position of the accent varies from word
to word. It falls on the first syllable in *tíka*, on the second in *kapâka*
and on the last in *tutá*. Each accented syllable carries one of the two tones,
acute or circumflex.

Pitch accent requires a `stress:` rule. Without one, no syllable is stressed,
so no syllable receives a tone either, which is rarely what you want.

Next: [Filters and Rejects](08-filters-and-rejects.md)
