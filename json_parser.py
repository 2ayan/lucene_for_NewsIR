#! /usr/bin/python
import json
import sys
from pprint import pprint
from os.path import basename
import codecs
import gzip
import re

fil=sys.argv[1]


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
        
            file_name=re.sub("\"","",source)
            file_name=re.sub("[ \t]+","_",file_name)
            file_name=re.sub(",[ \t]+","",file_name)
            file_name=re.sub("/","_",file_name)
            file_name=re.sub("^[ \t\r]*","",file_name)
            file_name=file_name.lower()
            
            if file_name == "." or file_name == ".."  or is_ascii(file_name) == False:
                continue

            file_name=("/c/NewsIR_Corpus/%s") % (file_name)

            output=("<DOC>\n<DOCNO>%s</DOCNO>\n<title>%s</title>\n<media_type>%s</media_type>\n<source>%s</source>\n<published>%s</published>\n<TEXT>%s</TEXT>\n</DOC>\n\n") % (ID,title,media_type,source,published,content)
            
            print "%s\n" % (file_name)

            f1 = codecs.open(file_name,'a', 'utf-8')
            f1.write(output)
            f1.close()
        
        except (ValueError, KeyError, TypeError) as e:
            print e

print "Converted to TREC format"
