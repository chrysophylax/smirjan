# 11. Compounds

Many languages express meanings by joining existing words. Var Ysalenn says
*keiz kair* 'body', literally "bones bloods". smirjan can generate compounds
like this. Compounds are optional; a language has none unless its `.def` asks:

```
compounds: yes
```

## Two kinds of compound

| kind | structure | example |
|---|---|---|
| **dvandva** (coordinative) | two coordinate parts, no head; together they name a whole | Var Ysalenn *keiz kair* 'bone + blood' = 'body'; Sanskrit *pāṇipādam* 'hands and feet' |
| **determinative** (tatpuruṣa) | a **head** saying what kind of thing it is, and a **modifier** narrowing it down | English *blackbird*, *firewood* |

smirjan's dvandvas are *samāhāra* dvandvas: the pair names a collective whole
rather than "A and B".

```
compound-types: dvandva determinative     # ranked, like every other list
compound-types: dvandva*3 determinative   # or weighted
```

The default is `determinative dvandva`, with determinatives more common.

## Head order

A determinative compound's head can come last (English *fire-wood*, a kind of
wood) or first (French *timbre-poste*, a kind of stamp). `compound-order:` sets
this. It's optional and defaults to `head-final`:

```
compound-order: head-final                  # always head last
compound-order: head-first                  # always head first
compound-order: head-final*95 head-first*5  # mostly head-final
```

As with every weighted list, the seed's jitter moves the exact split a little.
The Var Ysalenn example uses `head-final*95 head-first*5`; its 1460-meaning
lexicon has 43 head-final compounds and 1 head-first.

## How compounds are written

```
compound-separator: space     # keiz kair
compound-separator: hyphen    # keiz-kair
compound-separator: ·         # any other text
compound-separator: none      # keizkair (the default)
```

With a separator, each part is a word of its own and keeps its own stress. With
no separator, the compound is a single word; see [Solid compounds](#solid-compounds).

## Compounds with meanings

Compounds make most sense together with a meaning list
([`--assign`](10-output-and-meaning-lists.md#--assign-meaning-lists)). A
language made from nothing has to build its compounds backwards:

1. Take a meaning from the list, say 'body'.
2. Find two meanings related to it: 'bone', 'blood'.
3. Give each of them a word. If a meaning already has a word, reuse it;
   otherwise generate a new simple word.
4. Join the two words: *keiz kair* 'body'.

Example ([`examples/11-compounds.def`](examples/11-compounds.def)):

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
tukni nan	the cave	= the furrow + the spring or well
teta nima	the lagoon	= the ocean + the water
lista nat	the low tide	= the high tide + the tide
kuli nenki	the mare	= the foal or colt + the stallion
nup taku	the mosquito	= the fly + the snake
kasem pa	the earlobe	= the ear + the gill
nemko tir	the udder	= the nipple or teat + the chest
maka kosi	the mill	= the mortar(1) + the pestle
reti mun	the cloak	= the grass-skirt + the clothing or clothes
si na	the ornament or adornment	= the jewel + the reef
…
```

In plain output a compound gets a third column listing its parts, in written
order. With `--tsv`, two columns are added: `compound` (`dvandva`,
`head-final` or `head-first`) and `components`:

| word | meaning_number | meaning | compound | components |
|---|---:|---|---|---|
| tukni nan | 14 | the cave | dvandva | the furrow + the spring or well |
| teta nima | 23 | the lagoon | head-first | the ocean + the water |
| lista nat | 28 | the low tide | dvandva | the high tide + the tide |
| kuli nenki | 171 | the mare | head-final | the foal or colt + the stallion |

*teta nima* 'lagoon' is head-first: the head *teta* 'ocean' comes first. *kuli
nenki* 'mare' is head-final: its head is *nenki* 'stallion'.

### Words stay consistent

Each meaning has exactly one simple word. A part used in several compounds is
the same word every time, and it's also that meaning's entry if the meaning is
on the list. In the 1460-meaning Var Ysalenn lexicon, *nod* 'the meal' is part
of the compounds for 'the lunch', 'the dinner' and 'the supper'. The same pair
of parts never makes two different compounds.

Parts that aren't among the meanings you asked for are added to the output as
entries of their own, so every part of every compound has a gloss. That's why
`… 300 --assign=wlt` prints a few more than 300 lines. The count you give is
the number of list meanings; the extra entries are the parts.

### Which meanings become compounds

- **Only things.** Only meanings in Concepticon's *Person/Thing* category are
  compounded, not actions, properties or numbers. A modifier can be a property
  ('to be late' + 'night' = 'darkness').
- **Basic words stay simple.** How often a meaning is compounded follows how
  often the world's languages use a complex word for it. WOLD records this for
  every meaning (Haspelmath & Tadmor 2009). 'fire' and 'water' are simple words
  in over 98 % of languages; 'potter' is complex in 40 %. `compound-rate`
  (default `25%`) is the chance for a meaning of average complexity. Simpler
  meanings get proportionally less, more complex ones more.
- **Only when there's a good pair.** If no two suitably related meanings exist,
  the meaning gets a simple word.

So the actual share of compounds is well below `compound-rate`. With
`compound-rate: 40%`, the example gives 149 compounds among the 1460 WOLD
meanings (10 %), and only 4 among the 100 Leipzig-Jakarta meanings. The
Leipzig-Jakarta list was designed as the vocabulary least likely to be
borrowed or analysable, so that's as it should be.

### Where the related meanings come from

Parts are mined from open cross-linguistic data, all CC BY 4.0 and bundled
with smirjan:

| source | what it gives | example |
|---|---|---|
| [Concepticon](https://concepticon.clld.org) relations | part/whole, broader/narrower, similar | *head* has parts *hair*, *ear*, *face*; an *old man* is a kind of *man* |
| Urban (2011), via Concepticon | meanings languages form from other meanings | *bird* from *animal*, *flame* from *tongue* |
| CLICS colexification communities (Rzymski et al. 2020), via [NoRaRe](https://norare.clld.org) | groups of meanings that languages often express with one word | *fire*, *wood*, *flame*, *firewood*, *candle* |
| Concepticon semantic fields | a boost for parts from the same field | *bone*, *blood* and *body* are all in "The body" |

- **Dvandva parts** are the meaning's parts, its kinds, and members of its
  colexification community. A second part from the same semantic field as the
  first is preferred.
- **A determinative's head** is a broader term if there is one ('molar tooth'
  → 'tooth', 'lunch' → 'meal'), otherwise a close relative. Its modifier is a part, a whole or a community
  member, and may be a property.
- **Kinship terms** only use explicit relations (*older sibling* = older sister
  + older brother), because colexification lumps nearly all kin terms together.
- A part is never one of the meaning's own glosses. 'leg/foot' isn't made
  from 'foot'.

The data is only a starting point. Some pairs will be inspired: 'east' = dawn
+ morning, 'lagoon' = ocean + water. Others will be merely odd: 'mosquito' =
fly + snake. Treat the output as a draft lexicon to curate: keep what you like,
re-seed or edit the rest.

## Compounds without meanings

Without `--assign`, `compounds: yes` makes `compound-rate` of the output
compounds of two words printed earlier. There are no meanings to guide the
choice, so this shows what compounds *look* like in the language:

```
$ ./smirjan docs/examples/11-compounds.def 16
ka
tes
mi
sosra
ku
ras
mi sosra
nas
ma
tuna
se
se ka
timtul
tes ku
ti
ras tuna
```

## Solid compounds

With `compound-separator: none` (the default), a compound is **one
phonological word** ([`examples/11-solid.def`](examples/11-solid.def)):

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
ˈta.sim.mu	the high tide	= the tide + the wave
ˈken.tu.pa.tam	the spring or well	= the hole + the cave
ˈsu.kuŋ.ka.pen	the fowl	= the bird + the cock/rooster
ˈta.ki.ka	the eyelash	= the eyebrow + the eyelid
```

- **The join follows the phonology.** [Cluster tables](09-cluster-tables.md)
  apply where the words meet (*su.kuŋ.ka.pen* above: *n* + *k* → *ŋk*), and a
  compound whose join breaks a rule is discarded.
- **Filters apply at the join only.** A [filter](08-filters-and-rejects.md)
  only rewrites matches that span the join, since each part was already
  filtered. Rules like `a > aa` don't apply twice.
- **Rejects** check the whole compound.
- **Stress** is assigned to the compound as a whole by the language's
  `stress:` rule, so each part loses its own stress.
- **Tone stays lexical.** Each syllable keeps the tone it has in its own word.
  Under [pitch accent](07-tone-and-pitch.md#pitch-accent), the compound's
  stressed syllable carries the accent of the word it came from.
- **Syllable boundaries stay** where the parts meet. A consonant ending the
  first word stays in its syllable rather than moving to a vowel-initial second
  word (*tagb* + *utaːta* → *tagb.u.taː.ta*), as at the compound boundaries of
  many languages.

## Keys

| key | default | meaning |
|---|---|---|
| `compounds` | `no` | `yes` to generate compounds |
| `compound-rate` | `25%` | with `--assign`: the chance for a meaning of average complexity; without: the share of output that is compounds |
| `compound-types` | `determinative dvandva` | ranked or weighted: `dvandva`, `determinative` (alias `tatpurusha`) |
| `compound-order` | `head-final` | ranked or weighted: `head-final`, `head-first` (alias `head-initial`) |
| `compound-separator` | `none` | `none`, `space`, `hyphen`, or any text |

Next: [A complete language](12-a-complete-language.md)
