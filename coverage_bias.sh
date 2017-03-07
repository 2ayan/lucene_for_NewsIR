#!/bin/bash

#grep "<DOCNO>" -R /c/NewsIR_Corpus/ | sed -e 's/:<DOCNO>/  /g' | sed -e 's/<\/DOCNO>//g' > /tmp/list_of_Docno
for i in {1..145};
do
    grep "^"$i" " --no-filename /c/lucene_for_NewsIR/data/qrels/qrel.res | tr 'A-Z' 'a-z' > /tmp/topics 
    ln=$(wc -l /tmp/topics| sed -e 's/ .*$//g')
    echo $ln > /dev/tty
    if [ $ln -eq 0 ] ;
	then 
	continue
	fi
    awk '{print $6}' /tmp/topics |  sort -u > /tmp/ns

    for j in `cat /tmp/ns`;
    do 
	awk -v ns=$j '{if($6==ns)print $5}' /tmp/topics > /tmp/docno

	l=$(wc -l /tmp/docno | sed -e 's/ .*$//g')
	rm -f /tmp/sc
	for k in `cat /tmp/docno` ;
	do
	    f=$(grep " $k"  /tmp/list_of_Docno | awk '{print $1}')
	    if [ -n "$f" ]; then
		cmd=$(echo "awk '/<DOCNO>$k/,/<\/TEXT>/ {print \$0;}' $f")

		eval $cmd | awk '/<TEXT>/,/<\/TEXT>/ {print $0}' | sed -e 's/<[^>]*>//g' >> /tmp/sc
	    fi
	done
	python /c/lucene_for_Codemix/stem_and_remove_stopwords.py /tmp/sc > /tmp/w
	w=$(wc -l /tmp/w | sed -e 's/ .*$//g')
	echo "$i $j $l $w"
    done
echo $i > /dev/tty
done
