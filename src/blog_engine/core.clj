(ns blog-engine.core
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    
    [me.raynes.fs :as fs])
  (:import java.text.SimpleDateFormat
           java.util.Date))


;;
;; Helper
;;


(defn path->filename
  [path]
  (-> (str/split path #"/")
       last))


(defn path->file-title
  [path]
  (-> (path->filename path)
       (str/split #"\.")
       first))


(defn path->link
  [href body]
  (format 
    "<a href='%s'>%s</a><br/>"
    href body))


(defn folder-seq
  [path]
  (let [folder
        (io/file path)
        
        files 
        (file-seq folder)
        
        files 
        (map str (rest files))]
    files))


(defn delete-directory-recursive
  "Recursively delete a directory."
  [^java.io.File file]

  (when (.isDirectory file)
    (run! delete-directory-recursive (.listFiles file)))

  (io/delete-file file))


(defn disable-root-link
  [content]
  (-> content
    (str/replace #"<a href='/'>" "<p>")
    (str/replace #"</a>" "</p>")))


(defn format-date [date-string]
  (let [date-format (SimpleDateFormat. "dd.MM.yyyy")]
    (.parse date-format date-string)))


(defn parse-date
  "finds a stamp like 19.04.1999 in text and returns its unix"
  [content]
  (let [date
        (re-find
          #"[0-3][0-9]\.[0-1][0-9]\.[0-2][0-9][0-9][0-9]"
          content)
        
        date
        (if date 
          (format-date date)
          (format-date "01.01.1970"))
        
        unix
        (.getTime date)]
    unix))


(defn compile-images
  [content]
  (let [img-regexp #"/img (.*)\n"]
    (str/replace 
      content 
      img-regexp
      (fn [s]
        (format "<img width='100&percnt;' src='assets/%s'>"
          (second
            (re-find img-regexp (first s))))))))


(comment
  
  (compile-images (slurp "text/туду.txt"))
  
  )


;;
;; Compilators
;;


(defn compile-post
  [{:keys [content head header footer out]}]
  (let
    
    [title
     (path->file-title content)
     
     head
     (slurp head)
     
     head
     (str/replace head
       "<head>"
       (format "<head>%s"
         (format "<title>%s</title>"
           title)))
     
     head
     (str/replace head
       "<head>"
       (format "<head>%s"
         (format "<meta name='description' content='%s'>"
          (reduce str (take 160 (slurp content))))))
     
     content
     (slurp content)
     
     content
     (compile-images content)
     
     content
     (str/trim content)
     
     content
     (str/replace content #"\n\n" "</p><p>")
     
     content
     (str/replace content #"\n" "<br/>")
     
     _ "Nobr space after any short words"
     content
     (str/replace
       content
       #"\b[\p{L}']{1,2}\b\ "
       (fn [s] (str/replace s " " "&nbsp;")))
     
     content
     (str/replace
       content
       " —"
       "&nbsp;—")
     
     
     
     content
     (str 
       head
       (slurp header)
       "<body>"
       
       "<h1>" title "</h1>"
       
       "<p>" content "</p>"
       
       "</body>"
       (slurp footer))]
    
    (spit (str out "/" title ".html") content)))


(defn compile-posts
  [{:keys [in] :as config}]
  (mapv
        (fn [path]
          (compile-post 
            (assoc config :content path)))
        (folder-seq in)))


(defn compile-index 
  [{:keys [in head header footer out]}]
  (spit (str out "/index.html")
    
    (str
      (slurp head)
      (disable-root-link (slurp header))
      
      
      (let [fs 
            (folder-seq in)
            
            sorted
            (map 
              (fn [f]
                {:file f
                 :unix (parse-date (slurp f))})
              fs)
            
            sorted
            (reverse (sort-by :unix sorted))
            
            fs 
            (map :file sorted)
            
            hs
            (map path->filename fs)
            
            hs
            (map 
              (fn [x] (str/replace x #"\.txt" ".html"))
              hs)
            
            ns
            (map path->file-title fs)
            
            links
            (map path->link hs ns)]
        (reduce str links))
      
      (slurp footer))))


(defn compile-blog
  [{:keys [out] :as config}]
  (if
    (.exists (io/file out))
    (do
      (delete-directory-recursive (java.io.File. out))))
  (.mkdir (java.io.File. out))
  (fs/copy-dir "resources/assets" out)
  (doto config
    (compile-index)
    (compile-posts)))


(defn -main
  [setup]
  (let [config(edn/read-string (slurp setup))]
    (compile-blog config)))



(comment
  
  
  
  
  (def CONFIG (edn/read-string (slurp "setup.edn")))
  (compile-blog
    CONFIG)
  

  
  
  (compile-post
    {
      :content
      "resources/sample.txt"
      
      :head
      "resources/head.html"
      
      :header
      "resources/header.html"
      
      :footer
      "resources/footer.html"
      
      :out
      "public"}
    ))























