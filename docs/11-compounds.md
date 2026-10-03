# 11. Compounds

A compound is a word formed by joining two existing words, and in many
languages compounding is one of the main sources of new vocabulary. Var
Ysalenn, the language built in the [next chapter](12-a-complete-language.md),
has *keiz kair* 'body', literally 'bones bloods'. This chapter describes the
kinds of compound smirjan can form, how their parts are ordered and written,
and how, given a meaning list, it chooses parts that make sense together.
Compounding is off unless the definition turns it on:

```
compounds: yes
```

## Two Kinds of Compound

The classification used here goes back to the Sanskrit grammarians, who
distinguished compounds by the relation between their parts. In a **dvandva**,
or coordinative compound, the two parts are of equal standing and neither is
the head; together they denote something that comprises both. In a
**determinative** compound (Sanskrit *tatpuruṣa*), one part, the **head**,
says what kind of thing is meant, and the other, the **modifier**, narrows it
down: a *blackbird* is a kind of bird, and *firewood* a kind of wood.

| kind | structure | example |
|---|---|---|
| **dvandva** (coordinative) | two coordinate parts, no head; together they name a whole | Var Ysalenn *keiz kair* 'bone + blood' = 'body'; Sanskrit *pāṇipādam* 'hands and feet' |
| **determinative** (tatpuruṣa) | a **head** stating what kind of thing is meant, and a **modifier** narrowing it down | English *blackbird*, *firewood* |

Dvandvas are of two sorts. An additive dvandva means 'A and B', whereas a
*samāhāra* (collective) dvandva names a whole of which A and B are
representative members, as *pāṇipādam* names hands and feet taken together.
smirjan forms only collective dvandvas, so each of its dvandvas has a meaning
of its own rather than standing for the pair.

The proportion of the two kinds is set by `compound-types:`, which is ranked
or weighted like any other list in a definition (see
[Explicit Weights](02-phonemes-and-frequency.md#explicit-weights)):

```
compound-types: dvandva determinative     # ranked, like every other list
compound-types: dvandva*3 determinative   # or weighted
```

The default, `determinative dvandva`, ranks determinatives first and therefore
makes them the more common kind.

## Head Order

Languages differ in where they place the head of a determinative compound.
English puts it last, so *firewood* is a kind of wood; French puts it first,
so *timbre-poste* is a kind of stamp. `compound-order:` sets the order for the
language being defined. It takes the same ranked or weighted values as other
lists and defaults to `head-final`:

```
compound-order: head-final                  # always head last
compound-order: head-first                  # always head first
compound-order: head-final*95 head-first*5  # mostly head-final
```

As with any weighted list, the seed's jitter shifts the exact proportions
slightly. The Var Ysalenn definition uses `head-final*95 head-first*5`, and
its 1460-meaning lexicon contains 43 head-final compounds and one head-first
compound. Dvandvas have no head, so the setting does not affect them.

## How Compounds Are Written

`compound-separator:` determines whether the parts are written as two words or
as one:

```
compound-separator: space     # keiz kair
compound-separator: hyphen    # keiz-kair
compound-separator: ·         # any other text
compound-separator: none      # keizkair (the default)
```

The choice is more than a matter of spelling. With a separator, each part
remains a word in its own right and keeps its own stress. Without one, the
compound is a single phonological word, to which the phonotactics and the
stress rule of the language apply afresh; this case is described under
[Solid Compounds](#solid-compounds).

## Compounds with Meanings

Compounds are most useful together with a meaning list
([`--assign`](10-output-and-meaning-lists.md#--assign-meaning-lists)). A
language invented from nothing has no history out of which its compounds could
have grown, so smirjan builds them backwards. For a meaning such as 'body' it
looks for two meanings related to it, say 'bone' and 'blood', gives each of
them a word, and joins the two words into *keiz kair* 'body'. A related meaning
that already has a word keeps it; one that has none receives a newly generated
simple word. The example
[`examples/11-compounds.def`](examples/11-compounds.def) applies this
procedure:

```
C: t k n s m l r p v
V: a i u e o

syllable: CV CVC
word-syllables: 1 2

compounds: yes
compound-rate: 40%
compound-types: dvandva determinative
compound-order: head-final*90 head-first*10
compound-separator: space
```

```
$ ./smirjan docs/examples/11-compounds.def 300 --assign=wlt | grep '= '
martan nan	the cave	= the furrow + the spring or well
kamti saka	the lagoon	= the ocean + the water
sita ni	the low tide	= the high tide + the tide
titla kiti	the mare	= the foal or colt + the stallion
taku ten	the mosquito	= the fly + the snake
pat situ	the earlobe	= the ear + the gill
katek kate	the nostril	= the beak + the nasal mucus
mirkap tir	the udder	= the nipple or teat + the chest
lamta nuta	the food	= the flour + the lunch
nuta mak	the dinner	= the lunch + the dough
…
```

In the plain output a compound has a third column, which lists its parts in
written order. With `--tsv`, two columns take its place: `compound`, which
gives the kind (`dvandva`, `head-final` or `head-first`), and `components`:

| word | meaning_number | meaning | compound | components |
|---|---:|---|---|---|
| martan nan | 14 | the cave | dvandva | the furrow + the spring or well |
| kamti saka | 23 | the lagoon | head-first | the ocean + the water |
| sita ni | 28 | the low tide | dvandva | the high tide + the tide |
| titla kiti | 171 | the mare | head-final | the foal or colt + the stallion |

The second and fourth rows show the two head orders. *kamti saka* 'lagoon' is
head-first, so its head *kamti* 'ocean' precedes the modifier *saka* 'water';
in the head-final *titla kiti* 'mare', the head *kiti* 'stallion' comes last.

### Words Stay Consistent

Each meaning is expressed by exactly one simple word throughout the lexicon. A
part that occurs in several compounds is the same word every time, and if its
meaning is also on the list, the same word appears as that meaning's own
entry. In the 1460-meaning Var Ysalenn lexicon, *tivun* 'the meal' is an entry
of its own and also the second part of the compounds for 'the lunch', 'the
dinner' and 'the supper'. A pair of parts is used only once, so no two
compounds are made of the same parts.

Parts whose meanings were not requested are added to the output as entries of
their own, which gives every part of every compound a gloss. This is why
`… 300 --assign=wlt` prints more than 300 lines (365 with this definition): the
count refers to meanings from the list, and the additional lines are parts
that compounds brought in.

### Which Meanings Become Compounds

Only meanings that denote things, those in Concepticon's *Person/Thing*
category, are expressed as compounds; actions, properties and numbers always
receive simple words. The modifier of a determinative compound may
nevertheless be a property, as in 'afternoon' = 'to be late' + 'night', which
this chapter's example produces with some seeds (two of 200 tried, among them
`seed: probe79`).

Among things, the likelihood of compounding follows the practice of the
world's languages. For each of its meanings WOLD records the proportion of
languages that express it with a simple, unanalysable word (Haspelmath &
Tadmor 2009): 'fire' and 'water' are simple words in over 98 % of languages,
whereas 'potter' is a complex word in 40 %. `compound-rate` (default `25%`) is
the probability of compounding a meaning of average complexity. For any other
meaning the probability is scaled by its complexity, that is, by the share of
languages that use a complex word for it, relative to the average, up to a
maximum of 100 %. Basic words are therefore rarely compounded, while meanings
that languages commonly analyse are compounded readily. A meaning that WOLD
does not score is compounded at `compound-rate` itself.

A meaning selected for compounding still receives a simple word if no two
suitably related meanings exist, or if every candidate compound breaks a rule
of the phonology. For all these reasons the actual share of compounds lies well
below `compound-rate`. With `compound-rate: 40%`, the example yields 135
compounds among the 1460 WOLD meanings, about 9 %, but only 4 among the 100
meanings of the Leipzig-Jakarta list. The second figure is to be expected:
the Leipzig-Jakarta list was compiled from the meanings least likely to be
borrowed or analysable.

### Where the Related Meanings Come From

The relations between meanings are taken from open cross-linguistic datasets,
all licensed CC BY 4.0 and bundled with smirjan:

| source | what it gives | example |
|---|---|---|
| [Concepticon](https://concepticon.clld.org) relations | part/whole, broader/narrower, similar | *head* has parts *hair*, *ear*, *face*; an *old man* is a kind of *man* |
| Urban (2011), via Concepticon | meanings languages form from other meanings | *bird* from *animal*, *flame* from *tongue* |
| CLICS colexification communities (Rzymski et al. 2020), via [NoRaRe](https://norare.clld.org) | groups of meanings that languages often express with one word | *fire*, *wood*, *flame*, *firewood*, *candle* |
| Concepticon semantic fields | a preference for parts from the same field | *bone*, *blood* and *body* are all in "The body" |

The two kinds of compound draw on these relations differently. The parts of a
dvandva may be the meaning's parts, its kinds (narrower terms), the meanings it
is formed from in Urban's data, similar meanings, or members of its
colexification community, with parts weighted most heavily and similar
meanings least. Candidates from the meaning's own semantic field count double,
and once the first part has been chosen, candidates from that part's field
count double again, which favours pairs such as 'bone' and 'blood'. The head
of a determinative compound is a broader term where one exists ('molar tooth'
has the head 'tooth', 'lunch' the head 'meal'), and otherwise a similar or
colexified meaning; its modifier is a part, a whole or a member of the
community, and may be a property.

Two further restrictions apply. Kinship terms are compounded from explicit
relations only (*older sibling* = *older sister* + *older brother*), because
CLICS places nearly all kin terms in a single community, which therefore says
little about any one of them. And a part is never one of the meaning's own
glosses, so 'leg/foot' is not made from 'foot'. Parts are always simple words;
a compound never contains another compound.

The relations describe tendencies across many languages, not facts about the
language being built, and the results vary accordingly. Some pairs are apt,
such as 'east' = 'dawn' + 'morning' and 'lagoon' = 'ocean' + 'water'; others,
such as 'mosquito' = 'fly' + 'snake', are merely odd. The output is best
treated as a draft lexicon: you can keep the compounds that work and edit the
rest by hand, or run again with another seed.

## Compounds without Meanings

Without `--assign` there are no meanings to guide the choice of parts, and the
compounds show only what compounds look like in the language. Each output word
is a compound with the probability `compound-rate`, formed from two different
simple words printed earlier. Compounding draws on a random stream of its own,
so turning it on leaves the simple words unchanged: below, the simple words are
exactly those the same definition prints with `compounds: no`, and the
compounds *mi sonra*, *tuna ka*, *tes ku* and *ras ma* are interleaved among
them.

```
$ ./smirjan docs/examples/11-compounds.def 16
ka
tes
mi
sonra
ku
ras
mi sonra
nas
sa
ma
tuna
tuna ka
se
tes ku
tistil
ras ma
```

## Solid Compounds

When `compound-separator` is `none`, the default, the parts are joined into
**one phonological word**, and the rules of the language apply to the result
as they would to any other word
([`examples/11-solid.def`](examples/11-solid.def)):

```
syllable: CV CVN
word-syllables: 1 2

stress: initial
syllable-separator: .

compounds: yes
compound-rate: 40%
compound-order: head-final

% p  t  k
m +  nt ŋk
n mp +  ŋk
```

```
ˈkum.mi	the shore	= the side + the corner
ˈta.ku.pum	the high tide	= the tide + the wave
ˈpen.siŋ.ka.to	the spring or well	= the hole + the cave
ˈni.neŋ.ki	the ash	= the dust + the steam
ˈkin.ta.ti.ki	the foal or colt	= the stallion + the mule
```

At the join, the phonotactics of the language take effect.
[Cluster tables](09-cluster-tables.md) govern the consonants that meet there,
so in *pen.siŋ.ka.to* the *m* that ends *pen.sim* 'hole' and the *k* that
begins *ka.to* 'cave' appear as *ŋk*; a compound whose join violates a rule is
discarded.
[Filters](08-filters-and-rejects.md) rewrite only matches that span the join,
because each part has already been filtered as a word of its own, and a rule
such as `a > aa` is therefore not applied twice to the same vowel. Rejects, by
contrast, examine the whole compound.

Stress and tone are treated differently from each other. Stress is assigned
anew by the language's `stress:` rule to the compound as a whole, so the parts
lose their own stress. Tone remains lexical: each syllable keeps the tone it
has in its own word, and under [pitch accent](07-tone-and-pitch.md#pitch-accent)
the compound's stressed syllable carries the accent of the word it comes from.

Syllable boundaries are kept where the parts meet. A consonant that ends the
first part stays in its syllable instead of becoming the onset of a
vowel-initial second part (*tagb* + *utaːta* → *tagb.u.taː.ta*), as happens at
compound boundaries in many languages.

## Keys

| key | default | meaning |
|---|---|---|
| `compounds` | `no` | `yes` to generate compounds |
| `compound-rate` | `25%` | with `--assign`: the chance for a meaning of average complexity; without: the share of output that is compounds |
| `compound-types` | `determinative dvandva` | ranked or weighted: `dvandva`, `determinative` (alias `tatpurusha`) |
| `compound-order` | `head-final` | ranked or weighted: `head-final`, `head-first` (alias `head-initial`) |
| `compound-separator` | `none` | `none`, `space`, `hyphen`, or any text |

Next: [A Complete Language](12-a-complete-language.md)
