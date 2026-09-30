# 4. Restrictions

Classes say which phonemes exist. Restrictions say which of them may appear
in a given slot.

## Exceptions

Square brackets after a class slot narrow it down:

| syntax | meaning |
|---|---|
| `C[-r]` | any `C` except `r` |
| `C[-r l]`, `C[-r,l]`, `C[-r-l]` | any `C` except `r` and `l` |
| `C[-rl]` | the same, if `rl` splits unambiguously into phonemes of `C` |
| `C[-N]` | any `C` that isn't in class `N` |
| `C[+p t]` | only `p` or `t` (they must belong to `C`) |
| `C[+P-p]` | members of class `P` except `p` |

`-` switches to excluding, `+` to allowing only, and the list can switch back
and forth. The phonemes keep their relative frequencies: excluding `r` doesn't
change how often `t` is picked compared to `k`.

An example ([`examples/04-exceptions.def`](examples/04-exceptions.def)):

```
C: t k n s m l r p ŋ
N: n m ŋ
V: a i u e o

# ŋ never starts a syllable, and r never starts a word.
syllable-initial: C[-ŋ r]V C[-ŋ r]VN
# Before a nasal coda, the onset is never a nasal itself.
syllable:         C[-ŋ]V C[-N]VN
# Word-final syllables end in n or m only.
syllable-final:   C[-ŋ]V C[-N]VN[+n m]
```

```
kintin takata meka matu kiŋta kaka kare sam suŋki nisate tata nita
```

`ŋ` still appears, but only as a coda (*kiŋ.ta*, *suŋ.ki*), and no word
starts with `r` or ends in `ŋ`.

An item that is neither a phoneme of the slot's class nor a class name is an
error:

```
smirjan: mylang.def: line 3: in shape 'C[-z]V' at position 2: 'z' is not a phoneme of class C nor a class
```

## Numbered slots: clusters and geminates

A number after a slot ties it to other slots in the same syllable:

- **The same number means the same phoneme.** `C1VC1` repeats one consonant; `C1C1` is a geminate.
- **Different numbers mean different phonemes.** `C1C2` is a true cluster, never a geminate.
- Slots without a number are unconstrained.

Compare these shapes with [`examples/04-numbered.def`](examples/04-numbered.def)
(`C: t k s p`, `V: a i u`, one-syllable words):

| shape | output |
|---|---|
| `CVCC` | tatk sups **titt** kast katk **tatt** satk **pakk** |
| `CVC1C2` | tats sups titk kast kats tatk sats pakt |
| `CVC1C1` | tatt tuss takk satt tutt sapp kuss kiss |
| `C1VC1` | tat pip tit sas kak kik tut pup |
| `C[-s]1VC1` | tat pip tit kak kik tut pup pap |

Plain `CVCC` sometimes produces geminates (in bold); `CVC1C2` never does, and
`CVC1C1` always does. Exceptions and numbers combine in either order:
`C[-s]1` and `C1[-s]`.

Notes:

- Numbers are **per syllable**. `C1` in one syllable has nothing to do with
  `C1` in the next.
- "Different" applies across classes too. In `C1N2`, the two slots never get
  the same phoneme, even if `C` and `N` share some.
- Numbers start at 1. Numbering literals or groups is an error.

## When a shape can't be filled

If a slot has no phonemes left (for example `C1VC2` when `C` has only one
phoneme), that attempt is thrown away and smirjan tries again with fresh random
choices. If nothing ever works, it gives up after a long run of failed attempts:

```
smirjan: only 0 distinct words could be generated from mylang.def
```

The same applies to everything else that can reject a word: cluster tables,
rejects and mora targets.

Next: [Word length and weight](05-word-length-and-weight.md)
