#!/usr/bin/env python3
"""Download only explicitly reviewed, licensed rows from metadata/images.csv.

Never searches and auto-labels. Any new rows require human review and provenance.
Per-host throttling is conservative; HTTPS only, bounded size, image decode gate.
"""
import argparse,csv,hashlib,io,time
from pathlib import Path
from urllib.parse import urlparse
from urllib.request import Request,urlopen
from PIL import Image,ImageOps
ROOT=Path(__file__).resolve().parents[1]
ALLOWED={'CC0 1.0','Public domain','CC BY 4.0','CC BY-SA 4.0','CC BY 3.0','CC BY-SA 3.0','CC BY 2.0','CC BY-SA 2.0'}
def main():
 p=argparse.ArgumentParser();p.add_argument('--metadata',default=str(ROOT/'dataset/metadata/images.csv'));p.add_argument('--delay',type=float,default=1.0);p.add_argument('--max-bytes',type=int,default=20_000_000);a=p.parse_args()
 path=Path(a.metadata); rows=list(csv.DictReader(path.open(newline='',encoding='utf-8'))); out=[]; known=set()
 for r in rows:
  if r.get('review_status','').find('accepted')<0: r['download_status']='needs_human_review';out.append(r);continue
  url=r.get('source_url',''); host=urlparse(url).hostname or ''
  if urlparse(url).scheme!='https' or not (urlparse(url).hostname or '').lower().endswith('wikimedia.org') or r.get('license','').strip() not in ALLOWED:
   r['download_status']='rejected_unverified_source_or_license';out.append(r);continue
  try:
   req=Request(url,headers={'User-Agent':'FabricFit-Ghana-AI/1.0 (dataset research; contact via repository)'})
   with urlopen(req,timeout=30) as resp:
    final_host=urlparse(resp.geturl()).hostname or ''
    if not final_host.lower().endswith('wikimedia.org'): raise ValueError('redirected outside Wikimedia host allowlist')
    data=resp.read(a.max_bytes+1)
   if len(data)>a.max_bytes: raise ValueError('image exceeds max bytes')
   digest=hashlib.sha256(data).hexdigest()
   if digest in known: r['download_status']='duplicate_sha256';out.append(r);continue
   im=Image.open(io.BytesIO(data));im.load();im=ImageOps.exif_transpose(im).convert('RGB')
   if min(im.size)<300: raise ValueError(f'image too small: {im.size}')
   suffix='.jpg'; dest=ROOT/'dataset/raw'/r['category']/(digest+suffix);dest.parent.mkdir(parents=True,exist_ok=True)
   im.save(dest,'JPEG',quality=95)
   r.update(file=str(dest.relative_to(ROOT)),width=im.width,height=im.height,sha256=digest,download_status='downloaded');known.add(digest)
   print('downloaded',r['id'],r['category'],im.size,host)
  except Exception as e:
   r['download_status']='download_error: '+str(e)[:250];print('SKIP',r.get('id'),r['download_status'])
  out.append(r);time.sleep(max(0,a.delay))
 fields=list(dict.fromkeys([k for row in out for k in row]))
 with path.open('w',newline='',encoding='utf-8') as f:w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(out)
 print('rows',len(out),'downloaded',sum(r.get('download_status')=='downloaded' for r in out))
if __name__=='__main__':main()
