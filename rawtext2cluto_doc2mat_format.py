#! /usr/bin/python
import json
import sys
from pprint import pprint
from os.path import basename
import codecs
import gzip
import re

fil=sys.argv[1]
fil1=sys.argv[2]

def is_ascii(s):
    return all(ord(c) < 128 and ord(c) >0 for c in s)

with codecs.open(fil, 'r', 'utf-8') as f:
    for line in  f:
        i=0
        try:
            data = json.loads(line)
            json.dumps(data, sort_keys=True, indent=4)

            ID=data['id']
            content=data['content']
            title=data['title']
            media_type=data['media-type']
            source=data['source']
            published=data['published']
        
            source=re.sub("\"","",source)
            source=re.sub("[ \t]+","_",source)
            source=re.sub(",[ \t]+","",source)
            source=re.sub("/","_",source)
            source=re.sub("^[ \t\r]*","",source)
            source=source.lower()
            
            if source != fil1:
                continue

            file_name=source
            file_name_data=("/c/cluto-2.1.2/newsIR_data/field_used=title.dat")
            file_name_map=("/c/cluto-2.1.2/newsIR_data/field_used=title.map") 


            title=title.replace("\n"," ").replace("\r"," ")

            print "%s\n" % (file_name)

            f1 = codecs.open(file_name_data,'a', 'utf-8')
            f1.write(title)
            f1.write("\n")
            f1.close()
            
            f1 = codecs.open(file_name_map,'a', 'utf-8')
            f1.write(source)
            f1.write(" ")
            f1.write(ID)
            f1.write("\n")
            f1.close()

        except (ValueError, KeyError, TypeError) as e:
            print e

print "Converted to TREC format"
