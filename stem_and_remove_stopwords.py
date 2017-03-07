#!/usr/bin/python
import nltk
from nltk.corpus import stopwords
import sys
import re
from nltk.tokenize import wordpunct_tokenize
from nltk.stem.porter import PorterStemmer
import string


porter = PorterStemmer()

stop_words = set(stopwords.words('english'))
stop_words.update(['.', ',', '"', "'", '?', '!', ':', ';', '(', ')', '[', ']', '{', '}','>','<','/','\\','`','~','#','$','%','^','&','*','_','-','|']) # remove it if you need punctuation 


st1=''
file=sys.argv[1]
#echo $file
lines = [line.rstrip('\n') for line in open(file)]
for line in lines:
    line = line.rstrip()
    if re.search('<title>', line) :
        line=line.replace("<title>", "").replace("</title>", "")
        line=line.translate(None, string.punctuation)
        list_of_stemmed_non_stop_words_in_line = [porter.stem(i.lower()) for i in wordpunct_tokenize(line) if i.lower() not in stop_words]
        st=' '.join(list_of_stemmed_non_stop_words_in_line);
        print st


