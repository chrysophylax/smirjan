# 7. Tone and pitch accent

Tone is optional. Without a `tones:` key, words carry no tone.

## Tones per syllable

`tones:` lists the tones, most common first. `-` stands for a tone that isn't
written, which is handy when one tone is left unmarked
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
tamo sa má la tan ti tu nú tin ni katu kú
```

A tone can be written as:

- a diacritic name (`acute`, `grave`, `macron`, `circumflex`, `caron`, … the
  same names as for [stress marks](06-stress.md#how-stress-is-written)),
- a combining character typed directly,
- tone letters (`˥ ˧ ˩`, `˧˥`),
- numbers or any other text (`1 2 3`, `H L`).

## Where the tone is written

`tone-position:` chooses:

| value | placement |
|---|---|
| `nucleus` | as a diacritic on the syllable's vowel (default when every tone is a diacritic) |
| `after` | after the syllable (default otherwise) |
| `before` | before the syllable |

Chao tone letters after each syllable ([`examples/07-tone-letters.def`](examples/07-tone-letters.def)):

```
syllable: CV
word-syllables: 1 2

tones: ˥ ˧ ˩ ˧˥ ˥˩
```

```
ke˥ su˥ ke˧ ki˥ta˧ ki˥ ka˧ka˧ sa˧ ku˧te˥ ta˥˩ ka˥
```

## Tone per mora

`tone-bearing:` decides which units carry a tone:

| value | meaning |
|---|---|
| `syllable` (default) | one tone per syllable |
| `mora` | one tone per mora of the nucleus |
| `stressed` | only the stressed syllable ([pitch accent](#pitch-accent)) |

With `tone-bearing: mora`, a long vowel weighing two morae gets two tones.
`tone-contour:` says how two tones on one vowel are written
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
nǐː níkà ní ká kà pá kǎː tásí síká tíː
```

- *nǐː*: low then high on a long vowel, written with the `caron` contour.
- *tíː*: high on both morae. A level tone is written once.
- A combination with no `tone-contour:` entry stacks both marks.
- If the nucleus is written with as many letters as it has tones (`aa`, `ai`),
  each letter gets its own tone instead of a contour: *páà*.

Only nucleus morae carry tone. Coda morae count for weight and stress, but not
for tone.

## Pitch accent

With `tone-bearing: stressed`, only the stressed syllable carries a tone; the
others are unmarked. Combined with `stress-mark: none`, the tone *is* the
accent ([`examples/07-pitch-accent.def`](examples/07-pitch-accent.def)):

```
syllable: CV CVC
word-syllables: 2 3

stress: free
stress-mark: none
tones: acute circumflex
tone-bearing: stressed
```

```
tíka kapâka tutî karamî nimsikát natmónti tukíni titú tisî takú
```

Pitch accent needs a `stress:` rule. With no stress, no syllable is stressed,
so no tone appears.

Next: [Filters and rejects](08-filters-and-rejects.md)
