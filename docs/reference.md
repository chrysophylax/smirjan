# Reference

This page lists the command-line options and every key of the definition
file, with their defaults and permitted values. It is meant to be consulted
rather than read through; each feature is introduced with examples in the
[tutorial](README.md).

## Command Line

```
smirjan [--tsv] [--assign=lpj|dlg|wlt] <definitions.def> <count>
smirjan --version
```

| option | description |
|---|---|
| `--tsv` | Prints tab-separated output with a header row ([details](10-output-and-meaning-lists.md#--tsv)) |
| `--assign=LIST`, `--assign LIST` | Pairs words with meanings from `lpj`, `dlg` or `wlt` ([details](10-output-and-meaning-lists.md#--assign-meaning-lists)); with `compounds: yes`, some meanings become [compounds](11-compounds.md) |
| `-h`, `--help` | Prints usage information. |
| `--version` | Prints the version: a release number, or `dev` for local builds. |

Exit status:

| status | meaning |
|---|---|
| 0 | success |
| 1 | missing, unreadable or invalid definition file |
| 2 | invalid arguments |
| 3 | fewer distinct words can be generated than requested |

## File Format

A definition file contains one `key: value` entry per line. A `#` begins a
comment when it stands at the start of a line or follows a space, so that `#`
can still occur inside a value. Keys of more than one letter are
case-insensitive, whereas a single-letter key is the name of a class and must
be an uppercase letter. Files are read as UTF-8 and normalised to NFC, so that a
phoneme typed with a precomposed character and the same phoneme typed with a
combining diacritic are treated as identical. An unknown key is an error
rather than being ignored, which catches misspelt keys.

Keys marked **repeatable** accumulate their values over several lines. For any
other key, the last line takes effect.

## Randomness and Frequency

| key | default | values |
|---|---|---|
| `seed` | random, printed to stderr | any text |
| `distribution` | `gusein-zade` | `gusein-zade`, `yule`, `zipf`, `flat` |
| `yule-b` | `0.5` | number |
| `yule-c` | `0.9` | positive number |
| `zipf-s` | `1` | number |
| `jitter` | `10%` | `0` up to (not including) `100%` |
| `random-rate` | `30%` | `0`–`100%`; default chance of optional elements |

Percentages can be written as `30%`, `30` or `0.3`.

Under `gusein-zade`, the phoneme of rank r in a class of n receives the weight
(1/n)(1/r + … + 1/n), the expected r-th largest frequency of a point drawn
uniformly from the simplex of frequency profiles (Gusein-Zade 1988, formula
(1)). These weights sum to one. The logarithmic approximation given in the same
paper is not used, because it sums to less than one
([proofs/GuseinZade.v](../proofs/GuseinZade.v)).

## Classes

| key | description |
|---|---|
| `A` … `Z` | Defines a class: phonemes separated by spaces, most frequent first. A class can include classes defined above it. `p*3` sets an explicit weight. |
| `nucleus` | Lists the classes whose slots form the syllable nucleus (default `V`). |

## Shapes

| key | description |
|---|---|
| `syllable` | Syllable shapes, most frequent first (**repeatable**). |
| `syllable-initial` | Shapes for the first syllable of a polysyllabic word (**repeatable**). |
| `syllable-medial` | Shapes for medial syllables (**repeatable**). |
| `syllable-final` | Shapes for the last syllable of a polysyllabic word (**repeatable**). |
| `syllable-mono` | Shapes for monosyllabic words (**repeatable**). |

Positional keys fall back to `syllable`. Shape syntax:

| syntax | meaning |
|---|---|
| `C` | a phoneme from class `C` |
| literal text | that phoneme |
| `X?` / `X?25` | optional, at the seeded default rate / at exactly 25% |
| `(…)` | optional group |
| `C1`, `C2` | same number = same phoneme; different numbers = different phonemes (per syllable) |
| `C[-r l]` | exclude phonemes; `C[-N]` excludes a class |
| `C[+p t]` | only these phonemes |

## Word Length and Weight

| key | default | meaning |
|---|---|---|
| `word-syllables` | `2 1 3` | lengths, most frequent first; ranges `1-3`, weights `3*0.5` |
| `word-morae` | none | required total weights, most frequent first |
| `morae` | `onset=0 nucleus=1 coda=1` | segment weights; also `X=2` for class `X` and `aː=2` for a phoneme (**repeatable**) |
| `heavy-morae` | `2` | morae required for a heavy syllable |

## Stress

| key | default | values |
|---|---|---|
| `stress` | `none` | `none`, `initial`, `second`, `penult`, `antepenult`, `final`, `heavy-penult` / `latin`, `heavy-final`, `leftmost-heavy`, `rightmost-heavy`, `free` |
| `stress-fallback` | `initial` / `final` | positional rule used when `leftmost-heavy` / `rightmost-heavy` finds no heavy syllable |
| `secondary-stress` | `none` | `none`, `alternating` |
| `stress-mark` | `ˈ` | spacing mark, diacritic name or combining character, or `none` |
| `secondary-stress-mark` | `ˌ` | as above |
| `stress-monosyllables` | `no` | `yes`, `no` |
| `syllable-separator` | none | text between syllables, e.g. `.` |

## Tone

| key | default | values |
|---|---|---|
| `tones` | none | tones, most frequent first; `-` = unmarked (**repeatable**) |
| `tone-bearing` | `syllable` | `syllable`, `mora`, `stressed` |
| `tone-position` | `nucleus` if all tones are diacritics, else `after` | `nucleus`, `after`, `before` |
| `tone-contour` | none | `acute+grave=circumflex …` (**repeatable**) |

Diacritic names: `acute`, `grave`, `macron`, `circumflex`, `caron`, `tilde`,
`double-acute`, `double-grave`, `breve`, `inverted-breve`, `diaeresis`, `ring`,
`dot`, `vertical-line`.

## Compounds

| key | default | values |
|---|---|---|
| `compounds` | `no` | `yes`, `no` |
| `compound-rate` | `25%` | with `--assign`: chance for a meaning of average complexity; without: share of output |
| `compound-types` | `determinative dvandva` | ranked or weighted `dvandva`, `determinative` |
| `compound-order` | `head-final` | ranked or weighted `head-final`, `head-first`, e.g. `head-final*95 head-first*5` |
| `compound-separator` | `none` | `none` (one solid word), `space`, `hyphen`, or any text |

See [Compounds](11-compounds.md).

## Phonotactics

| key | meaning |
|---|---|
| `filter` | `pattern > replacement`, several separated by `;`; `!` deletes (**repeatable**) |
| `reject` | patterns separated by spaces; a match discards the word (**repeatable**) |
| `%` table | a [cluster table](09-cluster-tables.md): `+` allow, `-` forbid, other text replaces |

Patterns are Java regular expressions; `{C}` matches any phoneme of class `C`.

Each word passes through the following stages in order: shapes → cluster
tables → filters → rejects → mora target → stress and tone. A filter therefore
sees the word after cluster tables have been applied, and a reject sees it
after filtering. When a solid compound is formed, the join between its members
passes through cluster tables, filters (only matches that span the join) and
rejects, and the compound is then stressed as a whole.
