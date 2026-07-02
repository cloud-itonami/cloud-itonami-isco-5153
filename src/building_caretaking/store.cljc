(ns building-caretaking.store
  "SSoT for the ISCO-08 5153 independent building-caretaking
  sole-proprietor actor, behind a `Store` protocol so the backend is a
  swap (MemStore default ‖ a real Datomic/kotoba-server backend, per the
  itonami actor pattern).

  Domain = independent building caretaking practice:

    building            — a registered building (buildingId, address,
                          hasTenants? boolean)
    maintenance-task     — a maintenance task under a building (taskId,
                          buildingId, category #{:routine :electrical
                          :plumbing :structural})
    incident-report      — an incident report under a building
                          (reportId, buildingId, severity)

  The append-only records are the operating ledger: a task or incident
  report must reference a registered building, and these records are
  never mutated in place, only appended.")

(defprotocol Store
  (building [st building-id])
  (tasks-of [st building-id])
  (incident-reports-of [st building-id])
  (register-building! [st building])
  (record-task! [st task])
  (record-incident-report! [st incident-report]))

(defrecord MemStore [state]
  Store
  (building [_ building-id]
    (get-in @state [:buildings building-id]))
  (tasks-of [_ building-id]
    (filter #(= building-id (:building-id %)) (:tasks @state)))
  (incident-reports-of [_ building-id]
    (filter #(= building-id (:building-id %)) (:incident-reports @state)))
  (register-building! [_ building]
    (swap! state assoc-in [:buildings (:building-id building)] building))
  (record-task! [_ task]
    (swap! state update :tasks (fnil conj []) task))
  (record-incident-report! [_ incident-report]
    (swap! state update :incident-reports (fnil conj []) incident-report)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:buildings {} :tasks [] :incident-reports []} seed)))))
