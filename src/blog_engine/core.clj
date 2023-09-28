(ns blog-engine.core
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    
    
    [me.raynes.fs :as fs]
    [hashids.core :as hashids]
    [clj-commons.digest :as digest]
    [pandect.algo.adler32 :as adler32])
  
  (:import java.text.SimpleDateFormat
           java.util.Date
           java.util.Base64
           java.text.Collator))


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
     (str/replace 
       content
       #"(?i)\b((?:https?:(?:/{1,3}|[a-zа-я0-9%])|[a-zа-я0-9.\-]+[.](?:рф|com|net|org|edu|gov|mil|aero|asia|biz|cat|coop|info|int|jobs|mobi|museum|name|post|pro|tel|travel|xxx|ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|Ja|sk|sl|sm|sn|so|sr|ss|st|su|sv|sx|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw)/)(?:[^\s()<>{}\[\]]+|\([^\s()]*?\([^\s()]+\)[^\s()]*?\)|\([^\s]+?\))+(?:\([^\s()]*?\([^\s()]+\)[^\s()]*?\)|\([^\s]+?\)|[^\s`!()\[\]{};:'\".,<>?«»“”‘’])|(?:(?<!@)[a-zа-я0-9]+(?:[.\-][a-zа-я0-9]+)*[.](?:рф|com|net|org|edu|gov|mil|aero|asia|biz|cat|coop|info|int|jobs|mobi|museum|name|post|pro|tel|travel|xxx|ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|Ja|sk|sl|sm|sn|so|sr|ss|st|su|sv|sx|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw)\b/?(?!@)))"
       (fn [s]
         (if 
           (re-find 
             #"http://|https://"
             (first s))
           (format 
             "<a href='%s'>%s</a>"
             (first s)  
             (first s))
           (format 
             "<a href='http://%s'>%s</a>"
             (first s)  
             (first s)))))
     
     content
     (str 
       head
       (slurp header)
       "<body>"
       
       "<h1>" title "</h1>"
       
       "<p>" content "</p>"
       
       "</body>"
       (slurp footer))]
    
    (spit (str out "/" (adler32/adler32 title) ".html") content)))


(comment
  (re-seq
    
    (slurp "text/туду.txt"))
  
  (re-seq
     #"[-a-zа-яА-ЯA-Z0-9@:%._\+~#=]{1,256}\.[a-zа-яА-ЯA-Z0-9()]{1,6}\b(?:[-a-zа-яА-ЯA-Z0-9()@:%_\+.~#?&//=]*)"
    (slurp "text/туду.txt"))
  
  
  (str/replace 
       (slurp "text/туду.txt")
       #"^https?:\/\/(?:www\.)?[-a-zа-яА-ЯA-Z0-9@:%._\+~#=]{1,256}\.[a-zа-яА-ЯA-Z0-9()]{1,6}\b(?:[-a-zа-яА-ЯA-Z0-9()@:%_\+.~#?&\/=]*)$"
       (fn [s]
         (format 
           "<a href='%s'>%s</a>"
           s s))))


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
              (fn [x] (->> (str/replace x #"\.txt" "")
                           adler32/adler32
                           (format "%s.html")))
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
  
  (hashids/encode 
    {:salt 
     "123"}
    "abobus")
  
  
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























