#!/usr/bin/env python3
"""Two-stage MobileNetV3Small training/evaluation and three TFLite exports.

Requires a verified 8-class dataset with at least 300 independent train photos
and nonempty group-disjoint validation/test splits. No smoke-data output is ever
presented as a real trained model.
"""
import argparse,csv,json,time
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
CLASSES=['KENTE','ANKARA_WAX_PRINT','ADINKRA','BATIK','TIE_DYE_ADIRE','LACE','GHANAIAN_FUGU_SMOCK','OTHER_UNKNOWN']
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--data',default=str(ROOT/'dataset/processed'));ap.add_argument('--epochs-head',type=int,default=20);ap.add_argument('--epochs-fine',type=int,default=15);ap.add_argument('--out',default=str(ROOT/'models'));ap.add_argument('--reports',default=str(ROOT/'reports'));a=ap.parse_args()
 import tensorflow as tf
 from sklearn.metrics import classification_report,confusion_matrix
 data=Path(a.data);out=Path(a.out);reports=Path(a.reports);out.mkdir(parents=True,exist_ok=True);reports.mkdir(parents=True,exist_ok=True)
 with (data/'labels.csv').open(newline='',encoding='utf-8') as f: rows=list(csv.DictReader(f))
 for split in ('train','validation','test'):
  c={k:sum(r['split']==split and r['label']==k for r in rows) for k in CLASSES}
  if not all(c.values()):raise SystemExit(f'refusing: every class needs independent {split} photos; got {c}')
 if min(sum(r['split']=='train' and r['label']==c for r in rows) for c in CLASSES)<300:raise SystemExit('refusing: minimum 300 train images/class required')
 def ds(split,shuffle):
  return tf.keras.utils.image_dataset_from_directory(data/split,image_size=(224,224),batch_size=32,labels='inferred',label_mode='int',class_names=CLASSES,shuffle=shuffle,seed=17)
 train,val,test=ds('train',True),ds('validation',False),ds('test',False)
 aug=tf.keras.Sequential([tf.keras.layers.RandomFlip('horizontal'),tf.keras.layers.RandomRotation(.08),tf.keras.layers.RandomZoom(.1),tf.keras.layers.RandomContrast(.12)],name='train_augmentation')
 inp=tf.keras.Input((224,224,3),name='rgb_0_255');x=aug(inp);base=tf.keras.applications.MobileNetV3Small(input_shape=(224,224,3),include_top=False,weights='imagenet');base.trainable=False;x=base(x,training=False);x=tf.keras.layers.GlobalAveragePooling2D()(x);x=tf.keras.layers.Dropout(.3)(x);outp=tf.keras.layers.Dense(8,activation='softmax',name='class_probabilities')(x);model=tf.keras.Model(inp,outp)
 callbacks=[tf.keras.callbacks.EarlyStopping(patience=5,restore_best_weights=True,monitor='val_loss'),tf.keras.callbacks.ReduceLROnPlateau(patience=2,factor=.3),tf.keras.callbacks.ModelCheckpoint(out/'fabricfit_best.keras',save_best_only=True,monitor='val_loss')]
 model.compile(tf.keras.optimizers.Adam(1e-3),loss='sparse_categorical_crossentropy',metrics=['accuracy']);hist1=model.fit(train,validation_data=val,epochs=a.epochs_head,callbacks=callbacks)
 base.trainable=True
 for layer in base.layers[:-30]:layer.trainable=False
 model.compile(tf.keras.optimizers.Adam(1e-5),loss='sparse_categorical_crossentropy',metrics=['accuracy']);hist2=model.fit(train,validation_data=val,epochs=a.epochs_fine,callbacks=callbacks)
 best=tf.keras.models.load_model(out/'fabricfit_best.keras');probs=best.predict(test);y=np.concatenate([y.numpy() for _,y in test]);pred=probs.argmax(1)
 report=classification_report(y,pred,labels=range(8),target_names=CLASSES,output_dict=True,zero_division=0);cm=confusion_matrix(y,pred,labels=range(8)).tolist();test_acc=float((y==pred).mean())
 (reports/'classification_report.json').write_text(json.dumps({'test_accuracy':test_acc,'classification_report':report,'confusion_matrix':cm,'class_order':CLASSES},indent=2)+'\n')
 hist={'stage_a':hist1.history,'stage_b':hist2.history};(reports/'training_history.json').write_text(json.dumps(hist,indent=2)+'\n')
 def converter():return tf.lite.TFLiteConverter.from_keras_model(best)
 (out/'fabricfit_fp32.tflite').write_bytes(converter().convert())
 c=converter();c.optimizations=[tf.lite.Optimize.DEFAULT];c.target_spec.supported_types=[tf.float16];(out/'fabricfit_fp16.tflite').write_bytes(c.convert())
 def representative():
  for x,_ in train.take(100):
   for image in x.numpy():yield [image[None,...].astype(np.float32)]
 c=converter();c.optimizations=[tf.lite.Optimize.DEFAULT];c.representative_dataset=representative;c.target_spec.supported_ops=[tf.lite.OpsSet.TFLITE_BUILTINS_INT8];c.inference_input_type=tf.uint8;c.inference_output_type=tf.uint8
 try:(out/'fabricfit_int8.tflite').write_bytes(c.convert());int8='success'
 except Exception as e:int8='failed: '+repr(e)
 from validate_tflite import validate
 comparisons={}
 for name in ('fabricfit_fp32.tflite','fabricfit_fp16.tflite','fabricfit_int8.tflite'):
  f=out/name
  if f.exists():comparisons[name]=validate(f,test,CLASSES)
 sizes={f.name:f.stat().st_size for f in out.glob('fabricfit_*.tflite')}
 (reports/'model_comparison.json').write_text(json.dumps({'test_accuracy_keras':test_acc,'tflite':comparisons,'bytes':sizes,'int8_conversion':int8},indent=2)+'\n')
 print(json.dumps({'test_accuracy':test_acc,'class_order':CLASSES,'models':sizes,'int8':int8},indent=2))
if __name__=='__main__':main()
