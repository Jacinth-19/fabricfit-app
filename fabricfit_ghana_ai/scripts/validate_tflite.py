#!/usr/bin/env python3
"""Run and validate TFLite tensor contract, probabilities, timing and test accuracy."""
import argparse,json,time
from pathlib import Path
import numpy as np
CLASSES=['KENTE','ANKARA_WAX_PRINT','ADINKRA','BATIK','TIE_DYE_ADIRE','LACE','GHANAIAN_FUGU_SMOCK','OTHER_UNKNOWN']
def validate(path,test_ds,classes=CLASSES):
 import tensorflow as tf
 interpreter=tf.lite.Interpreter(model_path=str(path));interpreter.allocate_tensors();inp=interpreter.get_input_details()[0];out=interpreter.get_output_details()[0]
 if tuple(inp['shape'])!=(1,224,224,3) or out['shape'][-1]!=len(classes):raise ValueError(f'bad tensor contract: {inp["shape"]}, {out["shape"]}')
 hits=n=0;lat=[];max_sum_error=0.0
 for batch,labels in test_ds:
  for im,y in zip(batch.numpy(),labels.numpy()):
   tensor=im[None,...]
   if inp['dtype']==np.uint8:
    scale,zero=inp['quantization'];tensor=np.clip(np.round(tensor/scale+zero),0,255).astype(np.uint8)
   elif inp['dtype']==np.int8:
    scale,zero=inp['quantization'];tensor=np.clip(np.round(tensor/scale+zero),-128,127).astype(np.int8)
   else:tensor=tensor.astype(inp['dtype'])
   interpreter.set_tensor(inp['index'],tensor);t=time.perf_counter();interpreter.invoke();lat.append((time.perf_counter()-t)*1000)
   pred=interpreter.get_tensor(out['index']).astype(np.float32)
   if out['dtype'] in (np.uint8,np.int8):
    scale,zero=out['quantization'];pred=(pred-zero)*scale
   pred=pred.reshape(-1);max_sum_error=max(max_sum_error,abs(float(pred.sum())-1.0))
   if not np.isfinite(pred).all() or np.any(pred<-.02) or np.any(pred>1.02):raise ValueError('non-finite or out-of-range probability output')
   hits+=int(pred.argmax()==int(y));n+=1
 return {'test_accuracy':hits/n if n else None,'samples':n,'input_shape':inp['shape'].tolist(),'input_dtype':str(inp['dtype']),'output_shape':out['shape'].tolist(),'output_dtype':str(out['dtype']),'max_probability_sum_error':max_sum_error,'median_latency_ms':float(np.median(lat)) if lat else None,'p95_latency_ms':float(np.percentile(lat,95)) if lat else None,'bytes':Path(path).stat().st_size}
def main():
 ap=argparse.ArgumentParser();ap.add_argument('model');ap.add_argument('--data',default='../dataset/processed/test');args=ap.parse_args()
 import tensorflow as tf
 ds=tf.keras.utils.image_dataset_from_directory(args.data,image_size=(224,224),batch_size=32,labels='inferred',label_mode='int',class_names=CLASSES,shuffle=False)
 print(json.dumps(validate(Path(args.model),ds),indent=2))
if __name__=='__main__':main()
