# Reference

## Command line

```
smirjan [--tsv] [--assign=lpj|dlg|wlt] <definitions.def> <count>
```

| flag | meaning |
|---|---|
| `--tsv` | tab-separated output with a header row ([details](10-output-and-meaning-lists.md#--tsv)) |
| `--assign=LIST`, `--assign LIST` | pair words with meanings from `lpj`, `dlg` or `wlt` ([details](10-output-and-meaning-lists.md#--assign-meaning-lists)) |
| `-h`, `--help` | usage |

Exit status: 0 success, 1 bad definition file, 2 bad arguments, 3 fewer
distinct words possible than requested.

## File format

`key: value`, one per line. `#` starts a comment at line start or after a
space. Multi-letter keys are case-insensitive. UTF-8, normalised to NFC.
Unknown keys are errors.

Keys marked **repeatable** add up over several lines; for other keys the last
line wins.

## Randomness and frequency

| key | default | values |
|---|---|---|
| `seed` | random, printed to stderr | any text |
| `distribution` | `gusein-zade` | `gusein-zade`, `yule`, `zipf`, `flat` |
| `yule-b` | `0.5` | number |
| `yule-c` | `0.9` | positive number |
| `zipf-s` | `1` | number |
| `jitter` | `10%` | `0` up to (not including) `100%` |
| `random-rate` | `30%` | `0`–`100%`; default chance of optional elements |

Percentages may be written `30%`, `30` or `0.3`.

## Classes

| key | meaning |
|---|---|
| `A` … `Z` | a class: phonemes separated by spaces, most frequent first. May include classes defined above it. `p*3` sets an explicit weight. |
| `nucleus` | classes whose slots form the syllable nucleus (default `V`) |

## Shapes

| key | meaning |
|---|---|
| `syllable` | syllable shapes, most frequent first (**repeatable**) |
| `syllable-initial` | shapes for the first syllable of a polysyllabic word (**repeatable**) |
| `syllable-medial` | shapes for middle syllables (**repeatable**) |
| `syllable-final` | shapes for the last syllable of a polysyllabic word (**repeatable**) |
| `syllable-mono` | shapes for one-syllable words (**repeatable**) |

Positional keys fall back to `syllable`. Shape syntax:

| syntax | meaning |
|---|---|
| `C` | a phoneme from class `C` |
| literal text | that phoneme |
| `X?` / `X?25` | optional, at the seeded default rate / at exactly 25 % |
| `(…)` | optional group |
| `C1`, `C2` | same number = same phoneme; different numbers = different phonemes (per syllable) |
| `C[-r l]` | exclude phonemes; `C[-N]` excludes a class |
| `C[+p t]` | only these phonemes |

## Word length and weight

| key | default | meaning |
|---|---|---|
| `word-syllables` | `2 1 3` | lengths, most frequent first; ranges `1-3`, weights `3*0.5` |
| `word-morae` | none | required total weights, most frequent first |
| `morae` | `onset=0 nucleus=1 coda=1` | segment weights; also `X=2` for class `X`, `aː=2` for a phoneme (**repeatable**) |
| `heavy-morae` | `2` | morae needed for a heavy syllable |

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

## Phonotactics

| key | meaning |
|---|---|
| `filter` | `pattern > replacement`, several separated by `;`; `!` deletes (**repeatable**) |
| `reject` | patterns separated by spaces; a match discards the word (**repeatable**) |
| `%` table | a [cluster table](09-cluster-tables.md): `+` allow, `-` forbid, other text replaces |

Patterns are Java regular expressions; `{C}` matches any phoneme of class `C`.

Processing order: shapes → cluster tables → filters → rejects → mora target →
stress and tone.
