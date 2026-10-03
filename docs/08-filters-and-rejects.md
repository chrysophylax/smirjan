# 8. Filters and Rejects

The mechanisms described so far, syllable shapes and their exceptions, work
within a single syllable. Many regularities of real languages are not of that
kind. A consonant may be palatalised before *i* regardless of which syllable
the *i* belongs to, and a language may forbid a sequence that only arises
where two syllables meet. Filters and rejects deal with such patterns. Both
are applied to the **whole word** once it has been assembled from its
syllables, and both are written as
[Java regular expressions](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/regex/Pattern.html)
with one addition: `{C}` matches any phoneme of class `C`, so that a pattern
can refer to a class instead of listing its members.

## Filters

A **filter** rewrites every match of a pattern. A rule consists of the
pattern, `>`, and the replacement:

```
filter: ti > tʃi
```

One `filter:` line can hold several rules separated by `;`, and the key can
be repeated on as many lines as needed. The rules form a single sequence and
are applied in the order in which they are written, each to the output of the
one before. A later rule therefore sees the effects of an earlier one, which
is how ordered sound changes are modelled. A rule placed after `ti > tʃi`
applies to the affricates that rule has created as well as to any that were
there before.

Two conventions concern the replacement. A replacement of `!`, or an empty
one, deletes the match, so `filter: h$ > !` removes a word-final *h*. Groups
captured by the pattern can be referred to as `$1`, `$2` and so on, which lets
a rule keep part of what it matched: `filter: ([aeiou])h > $1ː` replaces a
vowel followed by *h* with the corresponding long vowel.

## Rejects

A **reject** discards any word that matches one of its patterns:

```
reject: ^{C}u u$ ll.*ll
```

The patterns on a line are separated by spaces, so a pattern cannot itself
contain a space; since words never contain spaces, nothing is lost. `reject:`
can be repeated. A rejected word is not repaired but thrown away, and smirjan
generates another in its place. Rejects thus change the make-up of the
vocabulary rather than the form of individual words. Each rejected word also
counts as a failed attempt, so a reject that matches most words slows
generation down, and one that leaves too few possibilities can make smirjan
stop with fewer distinct words than were requested.

## Example

([`examples/08-filters.def`](examples/08-filters.def))

```
C: t k s n m l p
V: a i u e o

syllable: CV CVC
word-syllables: 2 3

filter: ti > tʃi; si > ʃi; {C}l > ll
reject: ^{C}u u$ ll.*ll
```

The first two filters palatalise *t* and *s* before *i*, and the third turns
any consonant followed by *l* into the geminate *ll*. The rejects remove words
that begin with a consonant and *u*, words that end in *u*, and words that
contain *ll* twice. The same seed with and without the `filter:` and `reject:`
lines gives:

| | output |
|---|---|
| without filters | kamtak nasa tekita kanat site maku titaso lati tase kani nitani titip |
| with filters | kamtak nasa tekita kanat ʃite tʃitaso latʃi tase kani nitani tʃitʃip tatta |

Four of these twelve words are palatalised (*ʃite*, *tʃitaso*, *latʃi*,
*tʃitʃip*). A fifth, *maku*, ends in *u* and is therefore rejected; it is
missing from the second list, and smirjan generates *tatta* at the end to
make up the number.
Counting over 2000 words shows the effect of every rule:

| | *ti* | *si* | *tʃi* | Cu- | -u | consonant + *l* | *ll* |
|---|---:|---:|---:|---:|---:|---:|---:|
| without | 417 | 216 | 0 | 285 | 238 | 46 | 1 |
| with | 0 | 0 | 461 | 0 | 0 | 0 | 43 |

No *ti* or *si* survives, and every consonant + *l* sequence has become *ll*
(*kalla*, *talla*, *senalli*). None of the remaining words begins with a
consonant followed by *u*, ends in *u*, or contains two *ll*, because any word
that did was rejected and replaced.

## Order of Operations

Each word passes through the following stages:

1. Syllables are generated from their shapes.
2. [Cluster tables](09-cluster-tables.md) are applied.
3. Filters are applied, in order.
4. Rejects are checked.
5. The [mora target](05-word-length-and-weight.md#words-of-a-fixed-weight) is checked.
6. [Stress](06-stress.md) and [tone](07-tone-and-pitch.md) are assigned and written.

Because marks are added at the last stage, patterns see only plain phonemes
and never `ˈ`, accents or syllable separators, and `^` and `$` match the start
and end of the word itself. The order also means that filters see the output
of cluster tables and rejects see the output of filters, so a reject can
remove words that a filter has made unacceptable. Since the mora target is
checked after filtering, a filter that deletes or lengthens segments affects
whether a word reaches its target weight.

In a solid [compound](11-compounds.md#solid-compounds), both parts have
already been filtered, so at the join a filter rewrites only matches that span
it.

Although a filter operates on the word as a string, its output is returned to
the syllable structure. The replacement text is assigned to the syllable in
which the match began and is split back into the defined phonemes, each of
which regains its class and its role as onset, nucleus or coda. In *latʃi*,
for example, `tʃ` is the onset of the second syllable and `i` its nucleus.
This is why stress and tone still fall on the vowel after filtering, and why
syllable weights are still counted correctly. A character that is not defined
as a phoneme attaches to the phoneme before it. Since only `t` is defined in
this example, `ʃ` attaches to it and the two form one onset. A filter can thus introduce
sounds that the inventory does not list, but the syllable structure stays as
you defined it.

Next: [Cluster Tables](09-cluster-tables.md)
