#!/usr/bin/env python3
"""Validate downloaded images, SHA-256 exact-deduplicate and report near-duplicates.

Near-duplicates require `imagehash` (pHash). The earliest row wins only for exact
byte hashes; perceptual matches are flagged for human review, never auto-deleted.
This keeps visual curation and copyright decisions auditable.
"""
import argparse,csv,hashlib,io,json
from collections import defaultdict
from pathlib import Path
from PIL import Image,ImageOps
ROOT=Path(__file__).resolve().parents[1]
def main():
 p=argparse.ArgumentParser();p.add_argument('--metadata',default=str(ROOT/'dataset/metadata/images.csv'));p.add_argument('--min-side',type=int,default=300);a=p.parse_args()
 path=Path(a.metadata);rows=list(csv.DictReader(path.open(newline='',encoding='utf-8')));seen={};near=[];invalid=[];valid=0
 try:
  import imagehash
 except ImportError:imagehash=None
 phashes=[]
 for r in rows:
  f=r.get('file','')
  if not f:continue
  imgpath=(ROOT/f).resolve()
  try:
   if ROOT.resolve() not in imgpath.parents: raise ValueError('file path escapes project root')
   data=imgpath.read_bytes(); digest=hashlib.sha256(data).hexdigest()
   if digest in seen:r['review_status']='rejected_exact_duplicate';r['duplicate_of']=seen[digest];continue
   seen[digest]=r.get('id','');im=Image.open(io.BytesIO(data));im.load();im=ImageOps.exif_transpose(im).convert('RGB')
   if min(im.size)<a.min_side: raise ValueError(f'too small {im.size}')
   r['width'],r['height']=map(str,im.size);r['sha256']=digest;valid+=1
   if imagehash:
    h=imagehash.phash(im)
    for old,old_id,old_cls in phashes:
     d=h-old
     if d<=6:near.append({'id':r.get('id'),'other':old_id,'distance':int(d),'same_label':r.get('category')==old_cls})
    phashes.append((h,r.get('id'),r.get('category')))
  except Exception as e:r['review_status']='rejected_invalid';r['rejection_reason']=str(e);invalid.append(r.get('id'))
 fields=list(dict.fromkeys(k for row in rows for k in row))
 with path.open('w',newline='',encoding='utf-8') as f:w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
 report={'rows':len(rows),'valid_unique':valid,'invalid':len(invalid),'near_duplicate_pairs':near,'near_duplicate_review_required':bool(near),'pHash_available':bool(imagehash),'warning':'Watermarks, fabric relevance and labels need manual visual review; hashes cannot certify them.'}
 (ROOT/'dataset/metadata/cleaning_report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()
