# 6. Stress

Stress is optional. Without a `stress:` key, no syllable is stressed and
nothing is marked.

## Fixed stress

```
stress: penult
syllable-separator: .
```

([`examples/06-stress.def`](examples/06-stress.def)) The same words under each
positional rule:

| `stress:` | output |
|---|---|
| `initial` | ˈna.te ˈta.se.kor ˈse.pa ˈtu.ma ˈtun.kak ˈnu.na.kul |
| `second` | naˈte taˈse.kor seˈpa tuˈma tunˈkak nuˈna.kul |
| `final` | naˈte ta.seˈkor seˈpa tuˈma tunˈkak nu.naˈkul |
| `penult` | ˈna.te taˈse.kor ˈse.pa ˈtu.ma ˈtun.kak nuˈna.kul |
| `antepenult` | ˈna.te ˈta.se.kor ˈse.pa ˈtu.ma ˈtun.kak ˈnu.na.kul |

When a word is too short for the rule, the stress moves to the syllable closest
to the one the rule names, so `antepenult` stresses the first syllable of a
two-syllable word.

`syllable-separator:` is optional. It's printed between syllables, except where
a stress mark already separates them (`taˈse.kor`, not `ta.ˈse.kor`).

## Weight-sensitive stress

These rules look at [syllable weight](05-word-length-and-weight.md#morae). A
syllable is **heavy** when it has at least `heavy-morae` morae (default 2).

| `stress:` | rule |
|---|---|
| `heavy-penult` (alias `latin`) | the penult if it's heavy, otherwise the antepenult |
| `heavy-final` | the final syllable if it's heavy, otherwise the penult |
| `leftmost-heavy` | the first heavy syllable; with none, `stress-fallback` (default `initial`) |
| `rightmost-heavy` | the last heavy syllable; with none, `stress-fallback` (default `final`) |
| `free` | a random syllable in each word: lexical stress |

`stress-fallback:` takes any positional rule (`initial`, `second`, `final`,
`penult`, `antepenult`).

A Latin-like system ([`examples/06-weight.def`](examples/06-weight.def)):

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

| word | syllables | weights | stressed syllable |
|---|---|---|---:|
| maˈlen.ka | ma.len.ka | 1.2.1 | 2 |
| talˈneː.meˌne | tal.neː.me.ne | 2.2.1.1 | 2 |
| ˈtoː.toˌkaː | toː.to.kaː | 2.1.2 | 1 |
| keˈtem.taˌtop | ke.tem.ta.top | 1.2.1.2 | 2 |
| ˌta.kaːˈla.taˌre | ta.kaː.la.ta.re | 1.2.1.1.1 | 3 |
| ˌti.saˈmiː.seː | ti.sa.miː.seː | 1.1.2.2 | 3 |
| ˈtem.saˌkiː | tem.sa.kiː | 2.1.2 | 1 |
| ˈne.naˌtat | ne.na.tat | 1.1.2 | 1 |

*ma**len**ka* has a heavy penult, so it's stressed; *toː**to**kaː* has a
light penult, so the stress falls on the antepenult.

## Secondary stress

`secondary-stress: alternating` adds secondary stress on every second syllable,
counting outwards in both directions from the primary stress (see the table
above). The default is `none`.

## How stress is written

| key | default | meaning |
|---|---|---|
| `stress-mark:` | `ˈ` | primary stress mark |
| `secondary-stress-mark:` | `ˌ` | secondary stress mark |
| `stress-monosyllables:` | `no` | whether one-syllable words are marked |

A **spacing** mark such as `ˈ` or `'` is written before the syllable. A
**diacritic** is placed on the syllable's nucleus instead. Use its name or the
combining character itself:

```
stress-mark: acute
secondary-stress-mark: grave
```

```
malénka talnéːmenè tóːtokàː ketémtatòp tàkaːlátarè tìsamíːseː témsakìː nénatàt
```

Available names: `acute`, `grave`, `macron`, `circumflex`, `caron`, `tilde`,
`double-acute`, `double-grave`, `breve`, `inverted-breve`, `diaeresis`,
`ring`, `dot`, `vertical-line`.

`stress-mark: none` computes stress without writing it. That's useful when the
orthography doesn't mark stress, but you still want `--tsv` to report it, or
want [pitch accent](07-tone-and-pitch.md#pitch-accent) to depend on it.

Next: [Tone and pitch accent](07-tone-and-pitch.md)
