(ns xtdb-repros.core
  (:require [xtdb.api :as xt]
            [clojure.walk :as walk])
  (:import java.util.UUID))

(defn model->xtdb-entity
  [measurement]
  (when measurement
    (let [add-namespace (fn [x]
                          (cond
                            (keyword? x) (keyword "measurement" (name x))
                            :else x))]
      ;; NOTE: it does not make any sense but using (:id measurement) as :xt/id will break XTDB
      ;; indexing mechanism and the entity will not be visible in subsequent queries.
      (->> (merge {:xt/id (:id measurement)
                   ;; Try the following as :xt/id
                   ;; {:measurement-id (get-in measurement [:id :measurement-id])
                    ;; :diagnostic-report-version-id (get-in measurement [:id :diagnostic-report-version-id])}
                   :cohesic/type :measurement
                   :measurement/measurement-id (get-in measurement [:definition :id])}
                  (walk/postwalk add-namespace (dissoc measurement :id :type :definition)))))))

(defn measurement-model
  []
  {:definition {:report-section-id "lv"
                :type :measurement-definition
                :label {:short "LV EDV"}
                :id "left-ventricle.end-diastolic-volume"
                :is-derived false :units {:label "ml" :system "si"}
                :legacy {:api {:id "lv/edv"} :appserver {:id "imaging.left-ventricle/end-diastolic-volume"}}
                :display-precision 0
                :value-type {:type "real" :dimensionality 1}}
   :diagnostic-report-id (str (UUID/randomUUID))
   :value [1.0 2.0 3.0]
   :type :measurement
   :measurement-id "left-ventricle.end-diastolic-volume"
   :id (with-meta
         {:measurement-id "left-ventricle.end-diastolic-volume" :diagnostic-report-version-id (str (UUID/randomUUID))}
         {::foo :bar})})

(with-open [n (xt/start-node {})]
  (let [tx (xt/submit-tx n (mapv #(vector ::xt/put %) [(model->xtdb-entity (measurement-model))]))]
    (xt/await-tx n tx)
    #_(xt/entity (xt/db n) {:measurement-id "left-ventricle.end-diastolic-volume"
                            :diagnostic-report-version-id "53cfbb74-beea-4630-b729-c0172591ee99"})
    (xt/q (xt/db n) '{:find [(pull e [*])] :where [[e :cohesic/type :measurement]]})))
