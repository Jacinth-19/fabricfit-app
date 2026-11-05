#!/usr/bin/env python3
"""Make group-disjoint 80/10/10 splits; fail the readiness gate if classes lack data."""
import argparse,csv,json,random,shutil
from collections import defaultdict,Counter
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CLASSES=['KENTE','ANKARA_WAX_PRINT','ADINKRA','BATIK','TIE_DYE_ADIRE','LACE','GHANAIAN_FUGU_SMOCK','OTHER_UNKNOWN']
def main():
 p=argparse.ArgumentParser();p.add_argument('--metadata',default=str(ROOT/'dataset/metadata/images.csv'));p.add_argument('--out',default=str(ROOT/'dataset/processed'));p.add_argument('--force',action='store_true');a=p.parse_args()
 rows=list(csv.DictReader(Path(a.metadata).open(newline='',encoding='utf-8')))
 rows=[r for r in rows if r.get('file') and r.get('download_status')=='downloaded' and not r.get('review_status','').startswith('rejected')]
 groups=defaultdict(list)
 for r in rows:groups[(r['category'],r.get('source_page') or r.get('sha256') or r['id'])].append(r)
 classgroups=defaultdict(list)
 for (c,g),items in groups.items():classgroups[c].append(items)
 out=Path(a.out)
 if out.exists():
  if not a.force:raise SystemExit(f'{out} exists; --force to replace')
  shutil.rmtree(out)
 counts={s:Counter() for s in ('train','validation','test')};manifest=[];rng=random.Random(17)
 for cls in CLASSES:
  gs=classgroups[cls][:];rng.shuffle(gs)
  # Deterministic group allocation, preserving each source group in exactly one split.
  n=len(gs); ntest=max(1,round(n*.1)) if n>=3 else 0; nval=max(1,round(n*.1)) if n>=3 else 0
  destinations=['train']*max(0,n-ntest-nval)+['validation']*nval+['test']*ntest
  for group,split in zip(gs,destinations):
   for row in group:
    src=(ROOT/row['file']).resolve()
    if ROOT.resolve() not in src.parents: raise SystemExit('metadata file path escapes project root')
    dest=out/split/cls/(src.stem+'.jpg');dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest)
    counts[split][cls]+=1;manifest.append({'path':dest.relative_to(out).as_posix(),'label':cls,'split':split,'group':row.get('source_page') or row.get('sha256'),'sha256':row.get('sha256','')})
 (out/'metadata').mkdir(parents=True,exist_ok=True)
 with (out/'labels.csv').open('w',newline='',encoding='utf-8') as f:w=csv.DictWriter(f,fieldnames=['path','label','split','group','sha256']);w.writeheader();w.writerows(manifest)
 deficits={c:{s:counts[s][c] for s in counts} for c in CLASSES if not all(counts[s][c] for s in counts)}
 report={'ratios_requested':[.8,.1,.1],'group_split':True,'images':sum(sum(v.values()) for v in counts.values()),'per_split':{s:dict(counts[s]) for s in counts},'classes_missing_a_split':deficits,'training_gate_passed':not deficits and all(counts['train'][c]>=300 for c in CLASSES),'claim_gate_passed':not deficits and all(counts['train'][c]>=1000 for c in CLASSES),'warning':'Small group counts may prevent exact ratios. Never split source-page groups or related captures across splits.'}
 (out/'metadata'/'split_report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()
