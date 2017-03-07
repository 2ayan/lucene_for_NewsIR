#!/bin/bash
sort -k1,1 data/queries/topics_NEWS_IR_2016_topic_no_and_category_name.txt | awk '{printf("%s %s",$1, $2);for(i=3;i<NF;i++)printf("_%s",$i);printf(" %s\n",$NF);}' > /tmp/t1
awk '{printf("%s %s %s %s %s %s",$1, $2,$3, $4,$5, $6); for(i=7;i<=NF;i++)printf("_%s",$i);printf("\n");}' $1|\
sort -k 1,1 | join -1 1 -2 1 - /tmp/t1 
