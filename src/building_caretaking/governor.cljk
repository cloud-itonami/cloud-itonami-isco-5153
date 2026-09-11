(ns building-caretaking.governor
  "BuildingCaretakingGovernor — the independent safety/traceability layer
  for the ISCO-08 5153 independent building-caretaking actor. The
  Caretaking Advisor proposes actions (maintenance-task, incident-
  report); it has no notion of building provenance or structural/
  electrical risk, so this MUST be a separate system able to *reject* a
  proposal and fall back to HOLD — the itonami-actor pattern (independent
  Governor gates a proposing actor) applied to this occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. A `:structural` or `:electrical`
  maintenance task ALWAYS requires human sign-off — it can never be
  auto-approved.

  HARD invariants for :building-caretaking/propose:
    1. Building provenance   — a maintenance-task or incident-report must
       reference a registered building.
    2. No-actuation          — the proposal must not directly mutate a
       task/incident-report record outside the record-task!/
       record-incident-report! path (effect must be :propose, never a
       raw store write).
    3. Structural/electrical safety — a maintenance-task whose `category`
       is `:structural` or `:electrical` always requires :high or higher
       safety-class, forcing human sign-off; it is never auto-approved
       regardless of confidence.
  SOFT:
    4. Confidence floor → escalate."
  (:require [building-caretaking.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])
(def high-risk-categories #{:structural :electrical})

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- high-risk-task? [proposal]
  (and (= :maintenance-task (:kind proposal))
       (contains? high-risk-categories (:category proposal))))

(defn- hard-violations [{:keys [building-fn]} proposal]
  (let [{:keys [building-id safety-class effect]} proposal
        found-building (building-fn building-id)]
    (cond-> []
      (nil? found-building)
      (conj {:rule :no-building :detail (str "未登録 building " building-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and (high-risk-task? proposal)
           (< (safety-rank (or safety-class :none)) (safety-rank :high)))
      (conj {:rule :structural-electrical-safety
             :detail "structural/electrical タスクは :high 以上の safety-class が必須"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:building-fn` lookup,
  decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 0.0)]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `building-caretaking.store/Store` implementation."
  [store]
  {:building-fn #(store/building store %)})
