(** * Gusein-Zade's rank-frequency formula sums to less than one

    Gusein-Zade (1988), "О распределении букв русского языка по частоте
    встречаемости", Problemy Peredachi Informatsii 24(4), 102-107, models the
    n letter frequencies of a language as a point distributed uniformly on the
    simplex {p_i > 0, sum p_i = 1}. The expected r-th largest frequency is the
    r-th coordinate of the centroid of the ordered sub-simplex, whose vertices
    are v_k = (1/k, ..., 1/k, 0, ..., 0) for k = 1..n (k non-zero entries):

      (1)  Mp(r) = (1/n) (1/r + 1/(r+1) + ... + 1/n),

    which the paper then approximates "by Euler's formula" as

      (2)  Mp(r) ~ (1/n) (ln (n+1) - ln r).

    This file proves, for every n >= 1:
    - [centroid_tail]: the centroid coordinate equals the right-hand side of (1);
    - [centroid_sum]:  the values (1) sum to exactly 1;
    - [gz_lt_centroid]: each value (2) is strictly smaller than the value (1);
    - [gz_sum_lt_1]:   hence the values (2) sum to strictly less than 1.

    The probabilistic step (expectation = centroid of the ordered simplex) is
    taken from the paper and not formalised here.

    smirjan uses formula (1). The second half of the file proves that this is
    sound for drawing phonemes: with weights that sum to one, the probability of
    drawing a phoneme is its weight, the order of the class is kept, and an
    exclusion such as C[-ŋ] keeps the proportions of the remaining phonemes.

    Checked with the Rocq Prover 9.0:
      docker run --rm -v "$PWD":/w:z -w /w rocq/rocq-prover:9.0 rocq compile GuseinZade.v *)

From Stdlib Require Import Reals Lra Lia.
Open Scope R_scope.

(** ** Finite sums: [sumR f m] = f 0 + ... + f (m-1). *)

Fixpoint sumR (f : nat -> R) (m : nat) : R :=
  match m with
  | O => 0
  | S m' => sumR f m' + f m'
  end.

Lemma sum_S : forall f m, sumR f (S m) = sumR f m + f m.
Proof. reflexivity. Qed.

Lemma sum_ext : forall f g m,
  (forall i, (i < m)%nat -> f i = g i) -> sumR f m = sumR g m.
Proof.
  intros f g m; induction m as [| m IH]; intros H; simpl; [reflexivity |].
  rewrite IH by (intros; apply H; lia). rewrite (H m) by lia. reflexivity.
Qed.

Lemma sum_plus : forall f g m,
  sumR (fun i => f i + g i) m = sumR f m + sumR g m.
Proof. intros f g m; induction m as [| m IH]; simpl; [lra | rewrite IH; lra]. Qed.

Lemma sum_scal : forall c f m, sumR (fun i => c * f i) m = c * sumR f m.
Proof. intros c f m; induction m as [| m IH]; simpl; [lra | rewrite IH; lra]. Qed.

Lemma sum_const : forall c m, sumR (fun _ => c) m = INR m * c.
Proof.
  intros c m; induction m as [| m IH]; simpl sumR; [simpl; lra |].
  rewrite IH, S_INR. lra.
Qed.

Lemma sum_le : forall f g m,
  (forall i, (i < m)%nat -> f i <= g i) -> sumR f m <= sumR g m.
Proof.
  intros f g m; induction m as [| m IH]; intros H; simpl; [lra |].
  assert (sumR f m <= sumR g m) by (apply IH; intros; apply H; lia).
  assert (f m <= g m) by (apply H; lia). lra.
Qed.

Lemma sum_lt : forall f g m, (0 < m)%nat ->
  (forall i, (i < m)%nat -> f i < g i) -> sumR f m < sumR g m.
Proof.
  intros f g m Hm H. destruct m as [| m]; [lia |]. simpl.
  assert (sumR f m <= sumR g m) by (apply sum_le; intros; apply Rlt_le, H; lia).
  assert (f m < g m) by (apply H; lia). lra.
Qed.

(** ** The model *)

(** Coordinate [r] (counted from 1) of the vertex v_k of the ordered simplex. *)
Definition vertex (k r : nat) : R := if (r <=? k)%nat then / INR k else 0.

(** Formula (1) as the paper derives it: coordinate [r] of the centroid of
    v_1, ..., v_n. *)
Definition centroid (n r : nat) : R := / INR n * sumR (fun i => vertex (S i) r) n.

(** Formula (2), the approximation used in practice. *)
Definition gz (n r : nat) : R := / INR n * (ln (INR (S n)) - ln (INR r)).

(** ** The centroid coordinate is (1/n)(1/r + ... + 1/n) *)

Lemma vertex_sum_below : forall q, sumR (fun i => vertex (S i) (S q)) q = 0.
Proof.
  intros q. rewrite (sum_ext _ (fun _ => 0)).
  - rewrite sum_const. lra.
  - intros i Hi. unfold vertex. destruct (S q <=? S i)%nat eqn:E; [apply Nat.leb_le in E; lia | reflexivity].
Qed.

Theorem centroid_tail : forall q m,
  sumR (fun i => vertex (S i) (S q)) (q + m) = sumR (fun j => / INR (S q + j)) m.
Proof.
  intros q m; induction m as [| m IH].
  - rewrite Nat.add_0_r. apply vertex_sum_below.
  - rewrite Nat.add_succ_r. simpl sumR. rewrite IH. f_equal.
    unfold vertex. replace (S q <=? S (q + m))%nat with true by (symmetry; apply Nat.leb_le; lia).
    reflexivity.
Qed.

(** ** Formula (1) sums to exactly 1 *)

Lemma double_sum : forall n,
  sumR (fun j => sumR (fun i => vertex (S i) (S j)) n) n = INR n.
Proof.
  intros n; induction n as [| n IH]; [simpl; lra |].
  (* Split off the new column i = n from every inner sum. *)
  rewrite (sum_ext _ (fun j => sumR (fun i => vertex (S i) (S j)) n + vertex (S n) (S j)))
    by (intros; reflexivity).
  rewrite sum_plus.
  (* The old block gains one row j = n, which is zero below the diagonal. *)
  rewrite sum_S, IH, vertex_sum_below.
  (* The new column contributes 1/(n+1) in each of its n+1 rows. *)
  rewrite (sum_ext _ (fun _ => / INR (S n))).
  - rewrite sum_const, Rinv_r by (apply not_0_INR; lia). rewrite S_INR. lra.
  - intros j Hj. unfold vertex.
    replace (S j <=? S n)%nat with true by (symmetry; apply Nat.leb_le; lia).
    reflexivity.
Qed.

Theorem centroid_sum : forall n, (1 <= n)%nat ->
  sumR (fun j => centroid n (S j)) n = 1.
Proof.
  intros n Hn. unfold centroid. rewrite sum_scal, double_sum.
  apply Rinv_l. apply not_0_INR. lia.
Qed.

(** ** Each value of formula (2) is smaller than the value of formula (1) *)

(** ln (1 + x) < x for x > 0. *)
Lemma ln_1_plus_lt : forall x, 0 < x -> ln (1 + x) < x.
Proof.
  intros x Hx. rewrite <- (ln_exp x) at 2.
  apply ln_increasing; [lra | apply exp_ineq1; lra].
Qed.

(** One step of the telescoping sum: ln (k+1) - ln k < 1/k. *)
Lemma ln_step : forall k, (1 <= k)%nat -> ln (INR (S k)) - ln (INR k) < / INR k.
Proof.
  intros k Hk. assert (Hpos : 0 < INR k) by (apply lt_0_INR; lia).
  replace (INR (S k)) with (INR k * (1 + / INR k))
    by (rewrite S_INR; field; lra).
  rewrite ln_mult by (try lra; assert (0 < / INR k) by (apply Rinv_0_lt_compat; lra); lra).
  assert (ln (1 + / INR k) < / INR k) by (apply ln_1_plus_lt, Rinv_0_lt_compat; lra).
  lra.
Qed.

(** ln (r+m) - ln r < 1/r + ... + 1/(r+m-1) for m >= 1, by telescoping. *)
Lemma ln_lt_harmonic : forall q m, (1 <= m)%nat ->
  ln (INR (S q + m)) - ln (INR (S q)) < sumR (fun j => / INR (S q + j)) m.
Proof.
  intros q m Hm. induction m as [| m IH]; [lia |].
  destruct m as [| m].
  - simpl sumR. rewrite Nat.add_0_r, Nat.add_1_r. rewrite Rplus_0_l.
    apply ln_step. lia.
  - assert (Hstep : ln (INR (S (S q + S m))) - ln (INR (S q + S m)) < / INR (S q + S m))
      by (apply ln_step; lia).
    specialize (IH ltac:(lia)).
    rewrite sum_S. replace (S q + S (S m))%nat with (S (S q + S m)) by lia.
    lra.
Qed.

Theorem gz_lt_centroid : forall n r, (1 <= r)%nat -> (r <= n)%nat ->
  gz n r < centroid n r.
Proof.
  intros n r Hr Hrn. destruct r as [| q]; [lia |].
  assert (Hn : 0 < / INR n) by (apply Rinv_0_lt_compat, lt_0_INR; lia).
  (* Write n = q + m with m >= 1, so that ranks r = q+1 .. n are the m terms. *)
  destruct (Nat.le_exists_sub (S q) n Hrn) as [m' [Hm' _]].
  set (m := S m'). assert (n = q + m)%nat as Hnqm by (unfold m; lia).
  unfold gz, centroid. apply Rmult_lt_compat_l; [exact Hn |].
  rewrite Hnqm at 2. rewrite centroid_tail.
  replace (S n) with (S q + m)%nat by lia.
  apply ln_lt_harmonic. unfold m; lia.
Qed.

(** ** Hence formula (2) sums to less than 1 *)

Theorem gz_sum_lt_1 : forall n, (1 <= n)%nat ->
  sumR (fun j => gz n (S j)) n < 1.
Proof.
  intros n Hn. rewrite <- (centroid_sum n Hn).
  apply sum_lt; [lia |]. intros j Hj. apply gz_lt_centroid; lia.
Qed.


(** * Drawing phonemes

    The phonemes of a class are written in order of decreasing frequency, so the
    phoneme at index [i] (counting from 0) has rank [i+1]. smirjan draws
    phonemes with [Weighted.pick]: among the phonemes a slot allows (an exclusion
    such as C[-ŋ] disallows some), it picks phoneme [i] with probability
    w_i / total, where [total] is the sum of the allowed weights.

    The section below holds for any non-negative weights:
    - the result is a probability distribution over the allowed phonemes
      ([pick_sum], [pick_excluded]);
    - it keeps the proportions between any two allowed phonemes ([pick_ratio]),
      and it is the only uniform rescaling that does so and sums to one
      ([pick_unique]);
    - a heavier phoneme remains strictly more probable ([pick_rank_order]);
    - weights that already sum to one are used unchanged ([pick_exact]);
    - weights that fall short of one, such as those of formula (2), are raised in
      proportion to their size ([pick_raises]), so the probabilities would no
      longer be the values of the formula. This is why smirjan uses (1).
    Jitter, which smirjan applies to the weights before picking, is not covered:
    it perturbs the weights by design, and a large jitter can swap two ranks. *)

Section Pick.

Variable n : nat.            (** number of phonemes in the class *)
Variable w : nat -> R.       (** weight of the phoneme at index i *)
Variable allowed : nat -> bool.  (** whether the slot admits phoneme i *)

Hypothesis w_nonneg : forall i, (i < n)%nat -> 0 <= w i.

Definition masked (i : nat) : R := if allowed i then w i else 0.
Definition total : R := sumR masked n.
(** Probability that [Weighted.pick] returns phoneme [i]. *)
Definition pick (i : nat) : R := masked i / total.

Theorem pick_sum : 0 < total -> sumR pick n = 1.
Proof.
  intros Ht. unfold pick, Rdiv.
  rewrite (sum_ext _ (fun i => / total * masked i)) by (intros; lra).
  rewrite sum_scal. fold total. field. lra.
Qed.

Theorem pick_excluded : forall i, allowed i = false -> pick i = 0.
Proof. intros i H. unfold pick, masked. rewrite H. unfold Rdiv. lra. Qed.

(** Proportions between allowed phonemes are those of the weights. *)
Theorem pick_ratio : forall i j, allowed i = true -> allowed j = true -> 0 < total ->
  pick i * w j = pick j * w i.
Proof.
  intros i j Hi Hj Ht. unfold pick, masked. rewrite Hi, Hj. field. lra.
Qed.

(** Scaling every allowed weight by a common factor gives a distribution only
    for the factor 1/total. *)
Theorem pick_unique : forall c, sumR (fun i => c * masked i) n = 1 -> c = / total.
Proof.
  intros c H. rewrite sum_scal in H. fold total in H.
  assert (total <> 0) by (intro Z; rewrite Z in H; lra).
  apply (Rmult_eq_reg_r total); [| assumption]. rewrite Rinv_l by assumption. lra.
Qed.

Theorem pick_rank_order : forall i j, allowed i = true -> allowed j = true ->
  0 < total -> w j < w i -> pick j < pick i.
Proof.
  intros i j Hi Hj Ht Hw. unfold pick, masked. rewrite Hi, Hj.
  unfold Rdiv. apply Rmult_lt_compat_r; [apply Rinv_0_lt_compat |]; lra.
Qed.

Theorem pick_raises : forall i, allowed i = true -> 0 < w i ->
  0 < total -> total < 1 -> w i < pick i.
Proof.
  intros i Hi Hw Ht Ht1. unfold pick, masked. rewrite Hi.
  apply (Rmult_lt_reg_r total); [lra |]. unfold Rdiv.
  rewrite Rmult_assoc, Rinv_l by lra. rewrite Rmult_1_r.
  rewrite <- (Rmult_1_r (w i)) at 2. apply Rmult_lt_compat_l; lra.
Qed.

(** Weights that sum to one are drawn with exactly their own probability. *)
Theorem pick_exact : total = 1 -> forall i, pick i = masked i.
Proof. intros H i. unfold pick. rewrite H. unfold Rdiv. rewrite Rinv_1. lra. Qed.

(** The total is positive as soon as one allowed phoneme has positive weight. *)
Lemma total_pos : forall k, (k < n)%nat -> allowed k = true -> 0 < w k -> 0 < total.
Proof.
  intros k Hk Ha Hw. unfold total.
  assert (Hnn : forall m, (m <= n)%nat -> 0 <= sumR masked m).
  { intros m; induction m as [| m IH]; intros Hm; simpl; [lra |].
    assert (0 <= masked m) by (unfold masked; destruct (allowed m); [apply w_nonneg; lia | lra]).
    specialize (IH ltac:(lia)). lra. }
  assert (Hgt : forall m, (k < m)%nat -> (m <= n)%nat -> 0 < sumR masked m).
  { intros m; induction m as [| m IH]; intros H1 H2; [lia |].
    rewrite sum_S. destruct (Nat.eq_dec k m) as [-> | Hne].
    - assert (0 <= sumR masked m) by (apply Hnn; lia).
      unfold masked at 2. rewrite Ha. lra.
    - assert (0 < sumR masked m) by (apply IH; lia).
      assert (0 <= masked m) by (unfold masked; destruct (allowed m); [apply w_nonneg; lia | lra]).
      lra. }
  apply Hgt; lia.
Qed.

End Pick.

(** ** Gusein-Zade weights in smirjan *)

(** A sum is strictly smaller if it is no larger term by term and strictly
    smaller in one term. *)
Lemma sum_lt_at : forall f g m k, (k < m)%nat ->
  (forall i, (i < m)%nat -> f i <= g i) -> f k < g k -> sumR f m < sumR g m.
Proof.
  intros f g m k Hk Hle Hlt. induction m as [| m IH]; [lia |].
  rewrite !sum_S. destruct (Nat.eq_dec k m) as [-> | Hne].
  - assert (sumR f m <= sumR g m) by (apply sum_le; intros; apply Hle; lia). lra.
  - assert (sumR f m < sumR g m) by (apply IH; [lia | intros; apply Hle; lia]).
    assert (f m <= g m) by (apply Hle; lia). lra.
Qed.

Lemma vertex_nonneg : forall k r, 0 <= vertex (S k) r.
Proof.
  intros k r. unfold vertex. destruct (r <=? S k)%nat; [| lra].
  apply Rlt_le, Rinv_0_lt_compat, lt_0_INR. lia.
Qed.

(** Weight of the phoneme at index [i] in a class of [n]: formula (1). *)
Definition w1 (n i : nat) : R := centroid n (S i).

Lemma w1_pos : forall n i, (i < n)%nat -> 0 < w1 n i.
Proof.
  intros n i Hi. unfold w1, centroid.
  apply Rmult_lt_0_compat; [apply Rinv_0_lt_compat, lt_0_INR; lia |].
  rewrite <- (Rmult_0_r (INR n)), <- sum_const.
  apply (sum_lt_at _ _ n i Hi).
  - intros j _. apply vertex_nonneg.
  - unfold vertex. replace (S i <=? S i)%nat with true by (symmetry; apply Nat.leb_le; lia).
    apply Rinv_0_lt_compat, lt_0_INR. lia.
Qed.

Lemma w1_nonneg : forall n i, (i < n)%nat -> 0 <= w1 n i.
Proof. intros. apply Rlt_le, w1_pos. assumption. Qed.

(** Formula (1) respects the order in which the phonemes are listed: the vertex
    v_{i+1} contributes 1/(i+1) to rank i+1 but nothing to any later rank. *)
Lemma w1_decreasing : forall n i j, (i < j)%nat -> (j < n)%nat -> w1 n j < w1 n i.
Proof.
  intros n i j Hij Hj. unfold w1, centroid.
  apply Rmult_lt_compat_l; [apply Rinv_0_lt_compat, lt_0_INR; lia |].
  apply (sum_lt_at _ _ n i ltac:(lia)).
  - intros k _. unfold vertex.
    destruct (S j <=? S k)%nat eqn:Ej; destruct (S i <=? S k)%nat eqn:Ei;
      try lra; try (apply Rlt_le, Rinv_0_lt_compat, lt_0_INR; lia).
    apply Nat.leb_le in Ej; apply Nat.leb_gt in Ei; lia.
  - unfold vertex.
    replace (S j <=? S i)%nat with false by (symmetry; apply Nat.leb_gt; lia).
    replace (S i <=? S i)%nat with true by (symmetry; apply Nat.leb_le; lia).
    apply Rinv_0_lt_compat, lt_0_INR. lia.
Qed.

(** With no exclusions, smirjan draws each phoneme with exactly the probability
    that Gusein-Zade's model assigns to its rank, and the first-listed phoneme
    is the most frequent. *)
Theorem smirjan_gusein_zade : forall n, (1 <= n)%nat ->
  let all := fun _ : nat => true in
  (forall i, (i < n)%nat -> pick n (w1 n) all i = centroid n (S i))
  /\ sumR (pick n (w1 n) all) n = 1
  /\ (forall i j, (i < j)%nat -> (j < n)%nat -> pick n (w1 n) all j < pick n (w1 n) all i).
Proof.
  intros n Hn all.
  assert (Htot : total n (w1 n) all = 1) by (apply centroid_sum; exact Hn).
  assert (Hpos : 0 < total n (w1 n) all) by lra.
  repeat split.
  - intros i Hi. rewrite (pick_exact n (w1 n) all Htot). reflexivity.
  - apply pick_sum. exact Hpos.
  - intros i j Hij Hj. apply pick_rank_order; try reflexivity; [exact Hpos |].
    apply w1_decreasing; assumption.
Qed.

(** With an exclusion such as C[-ŋ], the remaining phonemes again form a
    distribution and keep the proportions of formula (1) to one another. *)
Theorem smirjan_exclusion : forall n allowed k, (k < n)%nat -> allowed k = true ->
  sumR (pick n (w1 n) allowed) n = 1
  /\ (forall i j, allowed i = true -> allowed j = true ->
        pick n (w1 n) allowed i * w1 n j = pick n (w1 n) allowed j * w1 n i).
Proof.
  intros n allowed k Hk Ha.
  assert (Hpos : 0 < total n (w1 n) allowed)
    by (apply (total_pos n (w1 n) allowed (w1_nonneg n) k); [exact Hk | exact Ha | apply w1_pos; exact Hk]).
  split.
  - apply pick_sum. exact Hpos.
  - intros i j Hi Hj. apply pick_ratio; assumption.
Qed.

(** Had smirjan used formula (2), every phoneme would have been drawn more
    often than the formula states, because its weights sum to less than one. *)
Theorem approximation_not_reproduced : forall n i, (i < n)%nat ->
  let all := fun _ : nat => true in
  gz n (S i) < pick n (fun j => gz n (S j)) all i.
Proof.
  intros n i Hi all.
  assert (Hpos : forall j, (j < n)%nat -> 0 < gz n (S j)).
  { intros j Hj. unfold gz.
    apply Rmult_lt_0_compat; [apply Rinv_0_lt_compat, lt_0_INR; lia |].
    assert (ln (INR (S j)) < ln (INR (S n))) by
      (apply ln_increasing; [apply lt_0_INR; lia | apply lt_INR; lia]). lra. }
  assert (Hlt : total n (fun j => gz n (S j)) all < 1) by (apply gz_sum_lt_1; lia).
  assert (Ht : 0 < total n (fun j => gz n (S j)) all).
  { apply (total_pos n _ all (fun j Hj => Rlt_le _ _ (Hpos j Hj)) i); [exact Hi | reflexivity | apply Hpos; exact Hi]. }
  apply (pick_raises n (fun j => gz n (S j)) all i); [reflexivity | apply Hpos; exact Hi | exact Ht | exact Hlt].
Qed.

Print Assumptions smirjan_gusein_zade.
Print Assumptions smirjan_exclusion.
Print Assumptions approximation_not_reproduced.
