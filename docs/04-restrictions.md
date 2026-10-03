# 4. Restrictions

A class says which phonemes a language has; it says nothing about where each
of them may occur. Most languages restrict their phonemes by position. English
has /ŋ/ only at the end of a syllable, and many languages allow fewer
consonants in the coda than in the onset. This chapter describes the two
devices smirjan offers for such restrictions, exceptions and numbered slots,
and then explains what happens when a shape cannot be filled at all.

## Exceptions

An **exception** is a list in square brackets after a class slot. It narrows
the set of phonemes the slot may draw on, either by excluding some members of
the class or by admitting only the listed ones:

| syntax | meaning |
|---|---|
| `C[-r]` | any `C` except `r` |
| `C[-r l]`, `C[-r,l]`, `C[-r-l]` | any `C` except `r` and `l` |
| `C[-rl]` | the same, if `rl` splits unambiguously into phonemes of `C` |
| `C[-N]` | any `C` that isn't in class `N` |
| `C[+p t]` | only `p` or `t` (both must belong to `C`) |
| `C[+P-p]` | members of class `P` except `p` |

Within the brackets, `-` switches to excluding and `+` to admitting only, and
a single list may switch back and forth, as the last row shows. The phonemes
that remain keep their frequencies relative to one another. Excluding `r`
from a slot therefore doesn't change how often `t` is picked compared to `k`;
the probability that belonged to `r` is shared out in proportion.

The following definition
([`examples/04-exceptions.def`](examples/04-exceptions.def)) combines
exceptions with the positional keys of [chapter 3](03-syllable-shapes.md) to
restrict where `ŋ` and `r` may occur:

```
C: t k n s m l r p ŋ
N: n m ŋ
V: a i u e o

# ŋ never begins a syllable, and r never begins a word of two or more syllables.
syllable-initial: C[-ŋ r]V C[-ŋ r]VN
# The onset before a nasal coda is never a nasal itself.
syllable:         C[-ŋ]V C[-N]VN
# The last syllable of such a word ends in n or m, or in no consonant.
syllable-final:   C[-ŋ]V C[-N]VN[+n m]
```

```
tintin takata meka matu kiŋta kaka kare sam siŋki nisate tata nita
```

In this output `ŋ` still occurs, but only as a coda, in *kiŋ.ta* and
*siŋ.ki*, because every onset slot excludes it while the coda slot `N`
doesn't. The restrictions on `r` and on the final coda are a little narrower
than they may look. `syllable-initial:` and `syllable-final:` apply only to
words of two or more syllables, and a one-syllable word uses `syllable:`,
which allows both an initial `r` and a final `ŋ`. In a run of 3000 words
from this file, 15 begin with `r` (*ra*, *ran*, *rim*) and 19 end in `ŋ`
(*taŋ*, *kuŋ*), all of them monosyllables. A `syllable-mono:` line would
close the gap if the restriction is meant to hold for every word.

An item in the brackets must be either a phoneme of the slot's class or the
name of a class. Anything else is reported as an error, since it almost
always means a typing mistake:

```
smirjan: mylang.def: line 3: in shape 'C[-z]V' at position 2: 'z' is not a phoneme of class C nor a class
```

## Numbered Slots: Clusters and Geminates

Exceptions constrain a slot by itself. A **numbered slot** constrains it
relative to other slots in the same syllable. Two slots with the same number
always receive the same phoneme, so `C1VC1` repeats its consonant and `C1C1`
produces a **geminate**, a doubled consonant. Two slots with different numbers
always receive different phonemes, so `C1C2` is a true cluster and never a
geminate. Slots without a number are not constrained in either direction.

The table compares five shapes on
[`examples/04-numbered.def`](examples/04-numbered.def), which has `C: t k s p`,
`V: a i u` and one-syllable words:

| shape | output |
|---|---|
| `CVCC` | tatk sups **titt** kast katk **tatt** satk **pakk** |
| `CVC1C2` | tats sups titk kast kats tatk sats pakt |
| `CVC1C1` | tatt tiss takk satt titt sapp kuss kiss |
| `C1VC1` | tat pip tit sas kak kik tut pup |
| `C[-s]1VC1` | tat pip tit kak kik tut pup pap |

With plain `CVCC` the two coda consonants are chosen independently, so a
geminate turns up whenever the same consonant happens to be picked twice (the
bold words). `CVC1C2` rules this out, and `CVC1C1` makes it obligatory. The
last row shows how exceptions and numbers combine. The onset excludes `s`,
and since the coda repeats the onset, `s` disappears from both positions.
The two can be written in either order, so `C[-s]1` and `C1[-s]` are
equivalent.

Numbers apply within a single syllable. A `C1` in one syllable is unrelated
to a `C1` in the next, so cross-syllable patterns need the tools of
[chapter 9](09-cluster-tables.md) instead. The requirement that different
numbers mean different phonemes holds across classes as well. In `C1N2`, for
instance, the two slots never receive the same phoneme, even where `C` and `N` have members
in common. Numbering starts at 1, and only class slots can be numbered;
attaching a number to a literal or a group is an error.

## When a Shape Can't Be Filled

Restrictions can leave a slot with no phonemes at all. In `C1VC2`, for
example, the second slot has nothing to choose from when `C` has only one
member. smirjan doesn't treat this as an error in the definition, because in
general a shape that fails with one set of choices may succeed with another.
It discards the attempt and starts again with fresh random choices, and it
gives up only after 200,000 consecutive attempts without a new word, printing
the words it has found and exiting with status 3:

```
smirjan: only 0 distinct words could be generated from mylang.def
```

Every other mechanism that can reject a word behaves in the same way. This
includes cluster tables, rejects and mora targets, which are described in
the following chapters.

Next: [Word Length and Weight](05-word-length-and-weight.md)
