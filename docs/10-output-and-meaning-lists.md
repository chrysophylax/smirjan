# 10. Output formats and meaning lists

```
smirjan [--tsv] [--assign=lpj|dlg|wlt] <definitions.def> <count>
```

Flags can come before or after the file and count.

## Plain output

By default smirjan prints each word on its own line, with stress, tone and
syllable separators as the `.def` asks for.

## `--tsv`

`--tsv` prints a header row and then one tab-separated row per word, for
spreadsheets, scripts or a lexicon database:

```sh
./smirjan --tsv docs/examples/06-weight.def 1000 > words.tsv
```

| column | example | meaning |
|---|---|---|
| `word` | `talˈneː.meˌne` | the word as it would normally be printed |
| `phonemes` | `t a l n eː m e n e` | its phonemes, separated by spaces, without marks |
| `syllables` | `tal.neː.me.ne` | plain syllables, joined with `.` |
| `syllable_count` | `4` | |
| `morae` | `6` | total [weight](05-word-length-and-weight.md#morae) |
| `weights` | `2.2.1.1` | the weight of each syllable |
| `stress` | `2` | number of the stressed syllable, counting from 1; empty without stress |
| `tones` | `acute.grave` | each syllable's tone as written in `tones:`, joined with `.`; empty without tones |

In the `tones` column, a syllable with [mora tones](07-tone-and-pitch.md#tone-per-mora)
lists them joined with `+` (`grave+acute`). A syllable that carries no tone
(e.g. unaccented syllables under pitch accent) is empty. The unmarked tone
shows as `-`.

The columns are the same for every language, so files from different `.def`s
line up. Columns a language doesn't use are just empty.

## `--assign`: meaning lists

`--assign=LIST` (or `--assign LIST`) gives each word a meaning from a standard
basic-vocabulary list:

| `LIST` | list | meanings |
|---|---|---|
| `lpj` | Leipzig-Jakarta list (Tadmor 2009) | 100 |
| `dlg` | Dolgopolsky list (Dolgopolsky 1964) | 15 |
| `wlt` | Loanword Typology / WOLD meaning list (Haspelmath & Tadmor 2009) | 1460 |

```
$ ./smirjan docs/examples/06-stress.def 5 --assign=lpj
ˈna.te	mouth
taˈse.kor	to take
ˈtu.ma	long
ˈse.pa	to tie
ˈtun.kak	bird
```

- **The seed decides the pairing.** Same seed, same pairs; a different seed
  pairs differently.
- **The words don't change.** The same words come out with or without
  `--assign`; only the meanings are added.
- **Words are printed in the list's order**, so a full run reads like the
  published list.
- **Fewer words than meanings:** a seeded random selection of meanings is used,
  as above (5 of 100).
- **More words than meanings:** the extra words come last with no meaning, and
  a warning goes to standard error:

  ```
  smirjan: the Dolgopolsky list has 15 meanings; 2 words were left unassigned
  ```

To get one word for every meaning, ask for the list's size:

```sh
./smirjan mylang.def 100 --assign=lpj > leipzig-jakarta.txt
./smirjan mylang.def 1460 --assign=wlt --tsv > wold.tsv
```

With `--tsv`, two columns are added at the end: `meaning_number` (the
number in the published list) and `meaning`. With `compounds: yes`, two more
follow, `compound` and `components` ([chapter 11](11-compounds.md)). A
compound written as separate words lists each column per part, joined with
` + ` (`di.tud + ir.ler`).

```
$ ./smirjan --tsv --assign=dlg docs/examples/06-stress.def 4
word	phonemes	syllables	syllable_count	morae	weights	stress	tones	meaning_number	meaning
ˈtu.ma	t u m a	tu.ma	2	2	1.1	1		3	second person marker
ˈna.te	n a t e	na.te	2	2	1.1	1		4	who/what
taˈse.kor	t a s e k o r	ta.se.kor	3	4	1.1.2	2		9	tooth
ˈse.pa	s e p a	se.pa	2	2	1.1	1		12	louse
```

The lists are bundled with smirjan, taken from
[Concepticon](https://concepticon.clld.org) (CC BY 4.0); see
[NOTICE.md](../NOTICE.md).

## Duplicates

A word is never printed twice. smirjan keeps generating until it has as many
distinct words as you asked for. If it goes a long time without finding a new
one, it stops, prints what it has, and says so. See
[chapter 2](02-phonemes-and-frequency.md#a-note-on-duplicates) for how this
affects frequencies in small phonologies.

## Exit status

| status | meaning |
|---:|---|
| 0 | success |
| 1 | the definition file is missing, unreadable or invalid |
| 2 | bad command-line arguments |
| 3 | fewer distinct words exist than were asked for (the ones found are still printed) |

Next: [Compounds](11-compounds.md)
