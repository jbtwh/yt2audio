(ns yt2audio.web
    (:gen-class)
    (:import [java.io InputStream InputStreamReader BufferedReader]
      [java.net URL HttpURLConnection Proxy InetSocketAddress Proxy$Type]
      [javax.naming InitialContext]
      [org.python.util PythonInterpreter]
      )
    (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
      [compojure.handler :refer [site]]
      [compojure.route :as route]
      [ring.adapter.jetty :as jetty]
      [ring.util.response :refer [redirect]]
      [environ.core :refer [env]]
      [clojure.java.io :refer [file output-stream input-stream resource]]
      )
    )

(defn getaudiolinksfrompl
  [plurl]
  (let [pi (new PythonInterpreter)]
    (.exec pi "import youtube_dl")
    (.exec pi "ydl_opts = {'quiet': False, 'skip_download': True, 'format': 'bestaudio',}")
    (.exec pi (format "info = youtube_dl.YoutubeDL(ydl_opts).extract_info('%s')" plurl))
    (.exec pi "print(info)")
    (.exec pi "entries = info['entries']")
    (.exec pi "print(len(entries))")
    (.exec pi "urls = []\nfor e in entries:\n    urls.append(e['url'])")
    (.exec pi "print(urls)")
    (.exec pi "for e in urls:\n    print(e)")
    (clojure.string/join "\n" (map #(str "<p><a href=\"" (.toString %) "\"/>" (.toString %) "</a></p>") (.toArray (.get pi "urls"))))))

(defn getaudiolink
      [url]
      (let [pi (new PythonInterpreter)]
        (.exec pi "import youtube_dl")
        (.exec pi "ydl_opts = {'quiet': False, 'skip_download': True, 'format': 'bestaudio[ext=m4a]',}")
        (.exec pi (format "info = youtube_dl.YoutubeDL(ydl_opts).extract_info('%s')" url))
        (.exec pi "print(info)")
        (.exec pi "audiourl = info['url']")
        (.exec pi "print(audiourl)")
        (.toString (.get pi "audiourl"))))

(defn notfound
      []
      {:status  404
       :headers {"Content-Type" "text/plain"}
       :body "Hello from Heroku"})

(defroutes app
           (GET "/video" request
             (redirect (getaudiolink (:r (:params request)))))
           (GET "/pl" request
             {:status  200
              :headers {"Content-Type" "text/html"}
              :body (getaudiolinksfrompl (:r (:params request)))})
           (ANY "*" request
             (notfound)))


;;(future (start-server :port 7888 :bind "0.0.0.0"))
;;(defonce server (start-server :port 7888 :bind "0.0.0.0"))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))