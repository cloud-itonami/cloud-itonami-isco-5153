(ns building-caretaking.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [building-caretaking.store :as store]
            [building-caretaking.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-building! st {:building-id "bldg-1" :address "1 Main St" :has-tenants? true})
    st))

(deftest proceeds-on-routine-task
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :maintenance-task :building-id "bldg-1" :category :routine
                   :safety-class :low :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-building
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :maintenance-task :building-id "no-such-building" :category :routine
                   :safety-class :low :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-building (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :maintenance-task :building-id "bldg-1" :category :routine
                   :safety-class :low :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest holds-on-electrical-task-without-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :maintenance-task :building-id "bldg-1" :category :electrical
                   :safety-class :medium :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :structural-electrical-safety (:rule %)) (:violations result)))))

(deftest human-approval-on-structural-task-with-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :maintenance-task :building-id "bldg-1" :category :structural
                   :safety-class :high :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :maintenance-task :building-id "bldg-1" :category :routine
                   :safety-class :none :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-task! st {:task-id "t1" :building-id "bldg-1" :category :routine})
    (store/record-incident-report! st {:report-id "r1" :building-id "bldg-1" :severity :low})
    (is (= 1 (count (store/tasks-of st "bldg-1"))))
    (is (= 1 (count (store/incident-reports-of st "bldg-1"))))))
