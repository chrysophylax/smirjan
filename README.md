# smirjan
[![CI/CD](https://github.com/chrysophylax/smirjan/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/chrysophylax/smirjan/actions/workflows/ci.yml)
smirjan generates words for constructed languages. The phonology of a
language is described in a definition file, a plain-text `.def` file that
lists the phonemes of the language in classes, the shapes its syllables may
take, how long its words are, and how stress, tone and phonotactic
restrictions apply. From that description smirjan produces as many distinct
words as are requested:

```
$ ./smirjan examples/mydef.def 8
þabˈhat.pe
ˈtʃi.tʃi
ˈaː.da
ˌhe.tʃiːˈkep.da
ˈbap.pa
ˈpá.piˌba
ˈta.pa
num
```

## Features

Phonemes within a class are listed from most to least frequent, and smirjan
assigns each a probability according to its rank. The probabilities follow
one of several rank-frequency distributions, the Gusein-Zade distribution by
default and alternatively Yule, Zipf or a flat distribution. The Gusein-Zade
distribution gives the phoneme of rank r in a class of n the expected r-th
largest frequency of a point drawn uniformly from the set of all possible
frequency profiles, (1/n)(1/r + … + 1/n) (Gusein-Zade 1988); these values
sum to one. The logarithmic approximation offered in the same paper is not
used, since it sums to less than one, as
[proofs/GuseinZade.v](proofs/GuseinZade.v) shows. A seeded jitter
then varies each probability slightly, so that classes of the same size do not
all share exactly the same frequencies. Because every random choice is derived
from the seed, a definition file with a fixed seed always produces the same
words.

Syllable shapes are written with class letters, as in `CV` or `CVN`. Elements
of a shape can be made optional, individual phonemes can be excluded from a
position (`C[-r]`), and numbered slots express clusters and geminates: `C1C2`
requires two different consonants, `C1C1` the same one twice. Word length is
given either as a number of syllables or as a number of morae. Stress can be
assigned to a fixed position or by syllable weight, optionally with secondary
stress, and tone can be carried by syllables or by morae, with contour tones
and pitch accent available. Phonotactic restrictions that cross syllable
boundaries are expressed as filters, rejects and cluster tables, following
the conventions of [Lexifer](https://lingweenie.org/conlang/lexifer/).

The `--tsv` option prints each word with its analysis into phonemes,
syllables, weights, stress and tone. The `--assign` option pairs words with
meanings from one of three standard lists: the Leipzig-Jakarta list, the
Dolgopolsky list or the Loanword Typology (WOLD) list. Meanings receive their
words in the order in which each list ranks them by basicness, and each
draws the length of its word from a Yule distribution that favours the
shortest words left. Basic meanings therefore tend to have short words, as
they do in natural languages, while seeded noise on the ranks leaves room
for exceptions. When compounds are enabled, some meanings are expressed as compounds of related meanings, which
smirjan finds in data derived from Concepticon and NoRaRe; *bee* and
*beehive*, for example, combine to give 'honey'. Compounds can be dvandva or
determinative, head-final or head-first, and written as one word or as
separate words.

The `--shift` option, which is used together with `--assign`, simulates
semantic change on the assigned words. A word acquires a new meaning along a
shift recorded for its old one in the Database of Semantic Shifts (Zalizniak
et al. 2024), such as 'tongue' to 'language', and a shift attested in many
language families is chosen more often than one attested in few. The new
meaning is carried either by the word itself, which thus becomes polysemous,
or by a word derived from it.

The bundled meaning data is licensed under CC BY 4.0. The sources and the
changes made to them are listed in [NOTICE.md](NOTICE.md).

## Installation

Native executables for Linux (x86-64), Windows (x86-64) and macOS (Apple
silicon) are published on the
[releases page](https://github.com/chrysophylax/smirjan/releases), together
with a portable jar that runs on any platform with Java 25 or later.

Building from source requires only JDK 25. The repository's `.sdkmanrc`
selects a suitable JDK for users of SDKMAN!:

```sh
sdk env                          # picks up .sdkmanrc (java=25.0.1-tem)
./build.sh                       # builds build/smirjan.jar
./smirjan examples/mydef.def 25
```

The command line takes a definition file and the number of words to
generate:

```
smirjan [--tsv] [--assign=lpj|dlg|wlt [--shift[=RATE]]] <definitions.def> <count>
smirjan --version
```

### Building with Mill

The project can also be built with [Mill](https://mill-build.org). The Mill
build downloads GraalVM CE 25 itself, so it does not depend on an installed
JDK, and in addition to the jar it can build a native executable:

```sh
./mill test.run      # self-tests
./mill jar           # out/jar.dest/out.jar
./mill nativeImage   # out/nativeImage.dest/native-executable
```

On Windows, `mill.bat` is used in place of `./mill`.

## Releases

Releases are numbered with integers that increase from one release to the
next, beginning at 954350000; the number carries no meaning beyond its order.
`smirjan --version` prints the number of the release, or `dev` for a build
that is not a release. Every push to `master` and every pull request is built
and tested on Linux, Windows and macOS. How releases are made, and how the
release process is protected against malicious pull requests, is described in
[RELEASING.md](RELEASING.md).

## Documentation

The [documentation](docs/README.md) is a tutorial. It begins with a definition
of three lines and adds one feature per chapter, ending with cluster tables and
the description of a complete language. Readers who need only the syntax of a
key or an option will find it in the one-page [reference](docs/reference.md).

## Examples

- [`examples/mydef.def`](examples/mydef.def): a tour of most features
- [`examples/var-ysalenn.def`](examples/var-ysalenn.def): an existing conlang, written in its orthography
- [`docs/examples/`](docs/examples/): one small file per documented feature

## Tests

```sh
./build.sh test
```

This command runs the self-tests and then every example file, which verifies
that each example still parses and generates words.
