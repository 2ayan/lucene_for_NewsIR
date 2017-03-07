#!/bin/bash
ls /c/NewsIR_Corpus/ | paste -d"\n" -s | sort -u > /tmp/list_of_source

for i in {1..145};
do
echo $i
grep "^"$i" " $1 | awk '{print $NF}' | tr 'A-Z' 'a-z' | sed -e 's/ /_/g' | sort -u > /tmp/list_in_res_file.txt
comm -23 /tmp/list_of_source /tmp/list_in_res_file.txt | sort -u > "/c/lucene_for_NewsIR/var/results/qn_"$i"_gateblocked.txt"
comm -12 /tmp/list_of_source /tmp/list_in_res_file.txt | sort -u > "/c/lucene_for_NewsIR/var/results/qn_"$i"_through_the_gate.txt"

done
