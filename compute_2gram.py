#!/usr/bin/python
import nltk
from nltk import bigrams
import sys

file=sys.argv[1]
st1=""
lines = [line.rstrip('\n') for line in open(file)]
st1='\n'.join(lines)


tokens = nltk.word_tokenize(st1)
tokens = [token.lower() for token in tokens if len(token) > 1] #same as unigrams
bi_tokens = bigrams(tokens)

print list(sorted(bi_tokens))
