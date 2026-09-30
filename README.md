# smirjan

A conlang word generator. You describe a phonology in a `.def` file (phoneme
classes, syllable shapes, word length, stress, tone, phonotactic rules) and
smirjan prints as many distinct words as you ask for.

```
$ ./smirjan examples/mydef.def 8
þabˈnat.pe
ˈtʃi.tʃi
ˈaː.ga
ˌhe.tʃiːˈkep.da
ˈbap.pa
ˈpá.piˌba
ˈta.pi
num
```

- Phonemes are picked by rank, following the Gusein-Zade or Yule
  rank-frequency distributions, with seeded jitter.
- Everything is reproducible from a seed.
- Syllable shapes support optional elements, exceptions (`C[-r]`), and
  numbered slots for clusters and geminates (`C1C2`, `C1C1`).
- Word length can be set in syllables or morae.
- Stress can be fixed or weight-sensitive, with optional secondary stress.
- Tone can go on syllables or morae, with contour tones and pitch accent.
- Phonotactics use filters, rejects, and cluster tables in the style of
  [Lexifer](https://lingweenie.org/conlang/lexifer/).
- `--tsv` gives structured output, and `--assign` pairs words with meanings
  from the Leipzig-Jakarta, Dolgopolsky or WOLD lists.
- Compounds can be dvandva or determinative, head-final or head-first, written
  solid or as separate words. With `--assign`, they're built from related
  meanings mined from Concepticon and NoRaRe (*bee* + *beehive* = 'honey').

The bundled meaning data is CC BY 4.0; see [NOTICE.md](NOTICE.md).

## Quick start

Requires JDK 27 and nothing else.

```sh
sdk env                          # picks up .sdkmanrc (java=27.0.0-amzn)
./build.sh                       # builds build/smirjan.jar
./smirjan examples/mydef.def 25
```

```
smirjan [--tsv] [--assign=lpj|dlg|wlt] <definitions.def> <count>
```

## Documentation

The [documentation](docs/README.md) is a tutorial that starts with a
three-line definition and ends with cluster tables and a complete language.
There's also a one-page [reference](docs/reference.md).

## Examples

- [`examples/mydef.def`](examples/mydef.def): a tour of most features
- [`examples/var-ysalenn.def`](examples/var-ysalenn.def): an existing conlang, written in its orthography
- [`docs/examples/`](docs/examples/): one small file per documented feature

## Tests

```sh
./build.sh test
```

This runs the self-tests, then every example file, to check that the examples
still parse and generate.
