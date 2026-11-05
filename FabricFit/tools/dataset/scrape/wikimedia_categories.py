#!/usr/bin/env python3
"""Collect openly licensed files from Commons' Ghanaian textile categories.

Categories identify plausible provenance, NOT verified image labels. Every image
is still review-pending and must be checked on the generated sheets. Only Commons
files whose own metadata passes the existing strict license/host gate are staged.
"""
from __future__ import annotations
import argparse,json,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
import common  # noqa: E402
from wikimedia import iter_candidates,API  # noqa: E402

CATEGORIES={
 "KENTE":["Category:Kente"],
 "ADINKRA":["Category:Adinkra","Category:Textile designs of Ghana"],
 "WAX":["Category:African wax prints","Category:Textiles of Ghana"],
 "ANKARA":["Category:African wax prints","Category:Textiles of Ghana"],
 "BATIK":["Category:Batik","Category:Textile designs of Ghana"],
 "TIEDYE":["Category:Adire","Category:Tie-dye textiles"],
 "LACE":["Category:Lace textiles"],
 "FUGU":["Category:Ghanaian smocks"],
}

def main():
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('--classes',default='KENTE,ADINKRA,WAX,ANKARA,BATIK,TIEDYE,LACE,FUGU')
 p.add_argument('--per-class',type=int,default=40)
 p.add_argument('--sleep',type=float,default=1.0)
 p.add_argument('--threshold',type=int,default=6)
 a=p.parse_args()
 available=common.load_classes();wanted=[x.strip().upper() for x in a.classes.split(',') if x.strip()]
 bad=set(wanted)-set(available)
 if bad:raise SystemExit(f'unknown class(es): {sorted(bad)}')
 session=common.make_session();stage=common.Staging('wikimedia_categories',available,threshold=a.threshold)
 for cls in wanted:
  n=0
  for category in CATEGORIES.get(cls,[]):
   cont={};seen=set()
   while n<a.per_class:
    params={'action':'query','generator':'categorymembers','gcmtitle':category,'gcmtype':'file','gcmnamespace':6,'gcmlimit':50,'prop':'imageinfo','iiprop':'url|extmetadata|size|mime','iiurlwidth':2048,'format':'json','formatversion':2,**cont}
    try:payload=json.loads(common.fetch_bytes(session,API,params=params))
    except Exception as e:common.log(f'! {category}: API error {e}');break
    pages=payload.get('query',{}).get('pages',[]) or []
    if not pages:break
    for page,reason in iter_candidates(payload):
     title=page.get('title','')
     if reason or title in seen:continue
     seen.add(title);info=page['_info'];ext=info.get('extmetadata') or {}
     try:data=common.fetch_bytes(session,info.get('thumburl') or info['url'])
     except Exception as e:common.log(f'! download {title}: {e}');continue
     img,why=common.validate_image(data,min_side=220)
     if img is None:continue
     outcome=stage.add(cls,img,title=title.replace('File:',''),creator=common.strip_html((ext.get('Artist') or {}).get('value','')),license_name=page['_license'],landing_url=info.get('descriptionurl',''),file_url=info.get('url',''),source_key=f"commons-category:{page.get('pageid',title)}",filename_hint=f'{cls}_{title}')
     if outcome=='ok':n+=1;common.log(f'+ {cls} <- {title} [{page["_license"]}] ({category})')
     if n>=a.per_class:break
    token=payload.get('continue') or {}
    if not token:break
    cont=token
    common.polite_sleep(a.sleep)
   if n>=a.per_class:break
  common.log(f'{cls}: {n} category candidates staged; {stage.stats}')
 print('Staging only. Candidate images are not accepted training data until every sheet is reviewed.')
 return 0
if __name__=='__main__':sys.exit(main())
