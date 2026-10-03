# 6. Stress

Stress is optional. A definition without a `stress:` key produces words in
which no syllable is stressed and nothing is marked. When stress is wanted,
smirjan offers two kinds of rule. A positional rule places stress on a
syllable counted from one edge of the word, as Polish places it on the
penult. A weight-sensitive rule takes the weight of syllables into account,
as Latin does. This chapter describes both kinds, then secondary stress, and
finally the ways in which stress can be written.

## Fixed Stress

A positional rule needs only the `stress:` key. The separator in the example
below is optional, but it makes the syllable boundaries visible
([`examples/06-stress.def`](examples/06-stress.def)):

```
stress: penult
syllable-separator: .
```

The table shows the same six words under each of the positional rules:

| `stress:` | output |
|---|---|
| `initial` | ˈna.te ˈta.se.kor ˈse.pa ˈti.ma ˈtun.kak ˈku.na.kil |
| `second` | naˈte taˈse.kor seˈpa tiˈma tunˈkak kuˈna.kil |
| `final` | naˈte ta.seˈkor seˈpa tiˈma tunˈkak ku.naˈkil |
| `penult` | ˈna.te taˈse.kor ˈse.pa ˈti.ma ˈtun.kak kuˈna.kil |
| `antepenult` | ˈna.te ˈta.se.kor ˈse.pa ˈti.ma ˈtun.kak ˈku.na.kil |

A word may be too short for the syllable a rule names. In that case the
stress falls on the closest syllable that does exist, so `antepenult`
stresses the first syllable of a two-syllable word. Since none of the words
here has more than three syllables, the `antepenult` row agrees with the
`initial` row throughout.

`syllable-separator:` is printed between syllables except where a stress mark
already stands at the boundary. smirjan writes `taˈse.kor` rather than
`ta.ˈse.kor`, since the mark itself shows where the syllable begins.

## Weight-Sensitive Stress

Weight-sensitive rules refer to the [weight of
syllables](05-word-length-and-weight.md#morae) as counted in the previous
chapter. A syllable is **heavy** when it has at least as many morae as
`heavy-morae` specifies (2 by default), and **light** otherwise.

| `stress:` | rule |
|---|---|
| `heavy-penult` (alias `latin`) | the penult if it's heavy, otherwise the antepenult |
| `heavy-final` | the final syllable if it's heavy, otherwise the penult |
| `leftmost-heavy` | the first heavy syllable; with none, `stress-fallback` (default `initial`) |
| `rightmost-heavy` | the last heavy syllable; with none, `stress-fallback` (default `final`) |
| `free` | a random syllable in each word (lexical stress) |

A word may contain no heavy syllable at all, and the two rules that search
for one then need somewhere else to put the stress. `stress-fallback:`
supplies that position and accepts any positional rule: `initial`, `second`,
`final`, `penult` or `antepenult`. The rule `free` differs from the others in
that it follows no pattern. It models **lexical stress**, where the position
of the stress is a property of each individual word, as in Russian.

The following definition reproduces the Latin rule
([`examples/06-weight.def`](examples/06-weight.def)):

```
C: t k n s m l r p
V: a e i o u
A: aː eː iː oː uː

nucleus: V A
morae: A=2
heavy-morae: 2

syllable: CV CVC CA
word-syllables: 3 4 5

stress: heavy-penult
secondary-stress: alternating
syllable-separator: .
```

Under the default counting a closed syllable such as *len* has two morae,
and the line `A=2` gives two morae to a syllable with a long vowel. Both are
therefore heavy, and only an open syllable with a short vowel is light. The
output, shown with the weights from `--tsv`:

| word | syllables | weights | stressed syllable |
|---|---|---|---:|
| maˈlen.ta | ma.len.ta | 1.2.1 | 2 |
| tamˈkeː.meˌne | tam.keː.me.ne | 2.2.1.1 | 2 |
| ˈtot.roˌti | tot.ro.ti | 2.1.1 | 1 |
| ˈki.kiˌta | ki.ki.ta | 1.1.1 | 1 |
| luˈkaː.leˌko | lu.kaː.le.ko | 1.2.1.1 | 2 |
| ˈta.reˌtam | ta.re.tam | 1.1.2 | 1 |
| tomˈlek.sa | tom.lek.sa | 2.2.1 | 2 |
| ˌteː.maˈkan.ke | teː.ma.kan.ke | 2.1.2.1 | 3 |

In *ma**len**ta* the penult is closed and therefore heavy, so it takes the
stress. In *tot**ro**ti* the penult is light, and the stress moves back to
the antepenult. The weight of the antepenult itself plays no part in the
rule: *ki.ki.ta* has a light antepenult and is still stressed on it.

## Secondary Stress

`secondary-stress: alternating` adds a secondary stress to every second
syllable, counting outwards in both directions from the primary stress. In
the table above, *tamˈkeː.meˌne* has its primary stress on the second
syllable and a secondary stress on the fourth, and *ˌteː.maˈkan.ke*, with
primary stress on the third syllable, has a secondary stress on the first.
The syllables adjacent to the primary stress are never stressed, so a
three-syllable word such as *ˈtot.roˌti* receives its secondary stress on the
final syllable. The default is `none`.

## How Stress Is Written

Three keys control the written form of stress:

| key | default | meaning |
|---|---|---|
| `stress-mark:` | `ˈ` | primary stress mark |
| `secondary-stress-mark:` | `ˌ` | secondary stress mark |
| `stress-monosyllables:` | `no` | whether one-syllable words are marked |

A stress mark is either spacing or a diacritic. A **spacing** mark, such as
`ˈ` or `'`, is written before the stressed syllable, which is the IPA
convention. A **diacritic** is placed on the nucleus of the syllable instead,
as Spanish orthography writes stress with an acute accent. A diacritic can
be given by name or typed directly as the combining character:

```
stress-mark: acute
secondary-stress-mark: grave
```

With these two lines, and with the `syllable-separator:` line removed, the
words of the Latin example are written as follows:

```
malénta tamkéːmenè tótrotì kíkità lukáːlekò táretàm tomléksa tèːmakánke
```

The available names are `acute`, `grave`, `macron`, `circumflex`, `caron`,
`tilde`, `double-acute`, `double-grave`, `breve`, `inverted-breve`,
`diaeresis`, `ring`, `dot` and `vertical-line`.

`stress-mark: none` computes the stress but doesn't write it. This is useful
for an orthography that leaves stress unmarked when you still want `--tsv`
to report it, and it is required for a
[pitch accent](07-tone-and-pitch.md#pitch-accent) that should depend on
stress without a separate stress mark.

Next: [Tone and Pitch Accent](07-tone-and-pitch.md)
