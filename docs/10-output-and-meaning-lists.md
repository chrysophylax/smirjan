# 10. Output Formats and Meaning Lists

This chapter describes what smirjan prints and the options that change it:
`--tsv`, which produces a table suitable for further processing, `--assign`,
which pairs words with meanings from a standard vocabulary list, and
`--shift`, which lets some of those words change their meaning. The full
command line is:

```
smirjan [--tsv] [--assign=lpj|dlg|wlt [--shift[=RATE]]] <definitions.def> <count>
smirjan --version
```

Options can come before or after the file and the count. `--version` prints
the version and exits.

## Plain Output

By default, smirjan prints each word on its own line, with whatever stress
marks, tone marks and syllable separators the `.def` file asks for.

## `--tsv`

`--tsv` prints a header row followed by one tab-separated row per word. The
format is meant for spreadsheets, scripts and lexicon databases, which need
the parts of a word in separate fields instead of a single marked-up string:

```sh
./smirjan --tsv docs/examples/06-weight.def 1000 > words.tsv
```

| column | example | meaning |
|---|---|---|
| `word` | `tamˈkeː.meˌne` | the word as it would normally be printed |
| `phonemes` | `t a m k eː m e n e` | its phonemes, separated by spaces, without marks |
| `syllables` | `tam.keː.me.ne` | plain syllables, joined with `.` |
| `syllable_count` | `4` | |
| `morae` | `6` | total [weight](05-word-length-and-weight.md#morae) |
| `weights` | `2.2.1.1` | the weight of each syllable |
| `stress` | `2` | number of the stressed syllable, counting from 1; empty without stress |
| `tones` | `acute.grave` | each syllable's tone as written in `tones:`, joined with `.`; empty without tones |

In the `tones` column, a syllable with
[mora tones](07-tone-and-pitch.md#tone-per-mora) lists them joined with `+`
(`grave+acute`). A syllable without a tone, such as an unaccented syllable
under pitch accent, has an empty entry, whereas the unmarked tone appears as
`-`; the two cases are therefore distinguishable.

The columns are the same for every language, and columns a language doesn't
use are left empty. Files produced from different `.def` files consequently
line up and can be compared or concatenated directly.

## `--assign`: Meaning Lists

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
ˈti.ma	long
ˈse.pa	to tie
ˈtun.kak	bird
```

The words themselves are those the definition produces without `--assign`.
The seed puts the meanings of the list into a random order, and the words
receive meanings from that order one by one, so that the first word
generated, *ˈna.te*, receives the first meaning drawn, 'mouth'. The pairs are
then printed in the order of the published list, which makes a complete run
read like the list itself. Because both the words and the order of the
meanings derive from the seed, the same seed always gives the same pairs,
and a different seed pairs the words differently.

When fewer words are requested than the list has meanings, only as many
meanings are drawn as there are words, which is why the example above uses 5
of the 100 Leipzig-Jakarta meanings. When more words are requested than the
list has meanings, the extra words are printed last, without a meaning, and a
warning goes to standard error:

```
smirjan: the Dolgopolsky list has 15 meanings; 2 words were left unassigned
```

To obtain exactly one word per meaning, request as many words as the list has
meanings:

```sh
./smirjan mylang.def 100 --assign=lpj > leipzig-jakarta.txt
./smirjan mylang.def 1460 --assign=wlt --tsv > wold.tsv
```

With `--tsv`, two columns are appended: `meaning_number`, the number of the
meaning in the published list, and `meaning`. With `compounds: yes`, two more
follow, `compound` and `components` (see [chapter 11](11-compounds.md)). For
a compound written as separate words, each column gives the values of the
parts joined with ` + `; in the Var Ysalenn example, for instance, the
`syllables` entry of *ylmiz dyl* 'valley' reads `yl.miz + dyl`.

```
$ ./smirjan --tsv --assign=dlg docs/examples/06-stress.def 4
word	phonemes	syllables	syllable_count	morae	weights	stress	tones	meaning_number	meaning
ˈti.ma	t i m a	ti.ma	2	2	1.1	1		3	second person marker
ˈna.te	n a t e	na.te	2	2	1.1	1		4	who/what
taˈse.kor	t a s e k o r	ta.se.kor	3	4	1.1.2	2		9	tooth
ˈse.pa	s e p a	se.pa	2	2	1.1	1		12	louse
```

The lists are bundled with smirjan and taken from
[Concepticon](https://concepticon.clld.org) (CC BY 4.0); see
[NOTICE.md](../NOTICE.md).

## `--shift`: Semantic Shifts

A **semantic shift** is a change by which a word for one meaning comes to
express another, as when a word for 'tongue' also comes to mean 'language'.
Many shifts recur in unrelated languages; the shift from 'tongue' to
'language', for instance, is attested in six language families. The
[Database of Semantic Shifts](https://datsemshift.ru) (DatSemShift; Zalizniak
et al. 2024) collects such shifts, and its version of 2024 links 4,583
meanings by 6,171 of them. `--shift` uses these records to simulate one round
of semantic change on the lexicon that `--assign` produces. Since only a word
with a meaning can change its meaning, `--shift` requires `--assign`, and
without it smirjan rejects the command line with exit status 2.

```
$ ./smirjan docs/examples/06-stress.def 8 --assign=lpj --shift=50%
ˈna.te	mouth	> face (polysemy)
ˈta.me	egg	> potato (derivation)
kuˈna.kil	new	> bride (derivation)
turˈmit.su	to know	> to recall, recollect (derivation)
taˈse.kor	to take	> to marry (polysemy)
ˈti.ma	long
ˈse.pa	to tie	> to begin (polysemy)
ˈtun.kak	bird	> omen (polysemy)
```

The words and their meanings are those printed without `--shift`, as a
comparison with the five-word example above shows. A word that has shifted
gains a last field, introduced by `>`, which gives its new meaning and the
way the shift is realised. Under **polysemy** the word itself comes to carry
the new meaning in addition to the old one, so that *taˈse.kor* means both
'to take' and 'to marry'. Under **derivation** the new meaning is expressed
by a word derived from the old one; a derivative of *ˈta.me* 'egg' thus
names the potato. smirjan does not build the derived word, because a
definition file describes no derivational morphology.

Each word whose meaning has at least one recorded shift undergoes a shift
with probability `RATE`, written as a percentage (`--shift=50%`) or as a
fraction (`--shift=0.5`); without a value, the rate is 25%. Words whose
meaning has no recorded shift keep it whatever the rate, and for this reason
the share of shifted words in a complete run is lower than `RATE` for the
larger lists. Shifts are recorded for 82 of the 100 Leipzig-Jakarta meanings
but for only 809 of the 1460 WOLD meanings. With the full WOLD list and the
default rate, the definition above shifts 210 words, about a quarter of those
809 and a seventh of the list.

The new meaning is drawn at random from the shifts that DatSemShift records
for the old one, and each shift is weighted by the number of language
families in which it is attested. The shift from 'fire' to 'electricity',
attested in four families by polysemy and in two by derivation, is thus
chosen more often than the shift from 'fire' to 'war', which is attested in
a single family. Polysemy and derivation are then chosen in proportion to
the number of families attesting each, so that 'fire' becomes 'electricity'
by polysemy twice as often as by derivation. The draws come from a stream of
their own, derived from the seed. The same seed therefore always produces
the same shifts, and adding `--shift` changes neither the words nor their
meanings.

With `--tsv`, two columns are appended after the others: `shift`, the new
meaning, and `shift_kind`, either `polysemy` or `derivation`. Both are empty
for a word that keeps its meaning.

```
$ ./smirjan --tsv --assign=dlg --shift docs/examples/06-stress.def 4
word	phonemes	syllables	syllable_count	morae	weights	stress	tones	meaning_number	meaning	shift	shift_kind
ˈti.ma	t i m a	ti.ma	2	2	1.1	1		3	second person marker		
ˈna.te	n a t e	na.te	2	2	1.1	1		4	who/what		
taˈse.kor	t a s e k o r	ta.se.kor	3	4	1.1.2	2		9	tooth		
ˈse.pa	s e p a	se.pa	2	2	1.1	1		12	louse	bad	derivation
```

At the default rate, one of the four words has shifted: a derivative of
*ˈse.pa* 'louse' means 'bad'. The other three rows end in two empty fields.

The new meanings carry the glosses of DatSemShift, which are often more
specific than those of the meaning lists and sometimes include a Latin name,
as in 'lily-of-the-valley (Convallaria majalis)'. The shift data is taken
from the DatSemShift list in Concepticon (CC BY 4.0); see
[NOTICE.md](../NOTICE.md).

## Duplicates

A word is never printed twice. smirjan keeps generating until it has as many
distinct words as you asked for. If 200,000 consecutive attempts produce no
new word, because every attempt either repeats a word or breaks a
constraint, it stops, prints the words it has found and reports the
shortfall with exit status 3. How the exclusion of duplicates affects
frequencies in small phonologies is discussed in
[chapter 2](02-phonemes-and-frequency.md#a-note-on-duplicates).

## Exit Status

| status | meaning |
|---:|---|
| 0 | success |
| 1 | the definition file is missing, unreadable or invalid |
| 2 | invalid command-line arguments |
| 3 | fewer distinct words exist than were requested (the ones found are still printed) |

Next: [Compounds](11-compounds.md)
