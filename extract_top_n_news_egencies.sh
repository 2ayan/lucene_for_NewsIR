#!/bin/bash
sort -k 1,1nr source.stat| head -n $1 > /tmp/tmp; 
mkdir -p /c/NewsIR_Corpus/top$1
while read r; 
do  
f=$(echo $r | sed -e 's/^[0-9]\+ //g'|\
sed -e 's/\"source\":\"//g' |\
sed -e 's/\"//g' -e 's/,$//g' | \
sed -e 's/ /_/g' | \
tr 'A-Z' 'a-z'); 

cp /c/NewsIR_Corpus/$f /c/NewsIR_Corpus/top$1/ 

done < /tmp/tmp 
echo "Corpus Created"

path=/c/NewsIR_Corpus/top$1
for f in `ls $path`; do 
python stem_and_remove_stopwords.py $path"/"$f;
done > "News_source_top"$1"_stem_remove_stopword_field_title.txt"

echo "Stemmed and stopwords removed"

python compute_2gram.py "News_source_top"$1"_stem_remove_stopword_field_title.txt" > /tmp/tmp1
echo "Bi-gram calculated"

sed 's/),/\)\n/g' /tmp/tmp1  | sed -e "s/[\',()\[\]]*//g"| sed -e 's/^[ \t]*//g' | uniq -c | sed -e 's/^[ \t]*//g' | sort -k 1,1nr > "2gram_most_freq_output_top"$1"News_source.txt"

echo "Bi-gram freq calculated"
