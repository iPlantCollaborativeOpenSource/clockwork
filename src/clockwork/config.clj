(ns clockwork.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clj-jargon.jargon :as jargon]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.infosquito.work-queue :as queue]
            [com.github.drsnyder.beanstalk :as beanstalk]))

(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(cc/defprop-str riak-base
  "The base URL used when connecting to Riak."
  [props config-valid configs]
  "clockwork.riak-base")

(cc/defprop-str tree-urls-bucket
  "The Riak bucket that is used to store tree URLs."
  [props config-valid configs]
  "clockwork.tree-urls-bucket")

(cc/defprop-str tree-urls-avu
  "The name of the AVU used to store the URL that refers to the tree URLs."
  [props config-valid configs]
  "clockwork.tree-urls-avu")

(cc/defprop-int tree-urls-cleanup-age
  "The mimimum age in days of a tree URL entry for it to be considered for cleanup."
  [props config-valid configs]
  "clockwork.tree-urls-cleanup-age")

(cc/defprop-str tree-urls-cleanup-start
  "The time of day in HH:MM:SS format that the tree URLs cleanup job should start every day."
  [props config-valid configs]
  "clockwork.tree-urls-cleanup-start")

(cc/defprop-str irods-host
  "The host name or IP address to use when connecting to iRODS."
  [props config-valid configs]
  "clockwork.irods-host")

(cc/defprop-str irods-port
  "The port number to use when connecting to iRODS."
  [props config-valid configs]
  "clockwork.irods-port")

(cc/defprop-str irods-user
  "The username to use when authenticating to iRODS."
  [props config-valid configs]
  "clockwork.irods-user")

(cc/defprop-str irods-password
  "The password t use when authenticating to iRODS."
  [props config-valid configs]
  "clockwork.irods-password")

(cc/defprop-str irods-home
  "The base path to the directory containing the home directories in iRODS."
  [props config-valid configs]
  "clockwork.irods-home")

(cc/defprop-str irods-zone
  "The name of the iRODS zone."
  [props config-valid configs]
  "clockwork.irods-zone")

(cc/defprop-optstr irods-resource
  "The name of the default resource to use in iRODS."
  [props config-valid configs]
  "clockwork.irods-resource")

(cc/defprop-str beanstalk-host
  "The hostname to use when connecting to Beanstalk."
  [props config-valid configs]
  "clockwork.beanstalk.host")

(cc/defprop-int beanstalk-port
  "The port number to use when connecting to Beanstalk."
  [props config-valid configs]
  "clockwork.beanstalk.port")

(cc/defprop-int beanstalk-connect-retries
  "The number of times to retry failed Beanstalk connection attempts."
  [props config-valid configs]
  "clockwork.beanstalk.connect-retries")

(cc/defprop-int beanstalk-task-ttr
  "The maximum amount of time that a Beanstalk task can be reserved."
  [props config-valid configs]
  "clockwork.beanstalk.task-ttr")

(cc/defprop-int infosquito-sync-interval
  "The number of hours between synchronization tasks for Infosquito."
  [props config-valid configs]
  "clockwork.infosquito.sync-interval")

(cc/defprop-str infosquito-beanstalk-tube
  "The tube to use when publishing work-queue items for Infosquito."
  [props config-valid configs]
  "clockwork.infosquito.beanstalk-tube")

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  []
  (cc/load-config-from-file (System/getenv "IPLANT_CONF_DIR") "clockwork.properties" props)
  (cc/log-config props)
  (validate-config))

(defn load-config-from-zookeeper
  "Loads the configuration settings from Zookeeper."
  []
  (cc/load-config-from-zookeeper props "clockwork")
  (cc/log-config props)
  (validate-config))

(defn jargon-config
  "Obtains a Jargon configuration map."
  []
  (jargon/init (irods-host) (irods-port) (irods-user) (irods-password) (irods-home) (irods-zone)
               (irods-resource)))

(defn beanstalk-queue
  "Obtains a beanstalk work-queue client."
  []
  (queue/mk-client
   #(beanstalk/new-beanstalk (beanstalk-host) (beanstalk-port))
   (beanstalk-connect-retries) (beanstalk-task-ttr) (infosquito-beanstalk-tube)))
