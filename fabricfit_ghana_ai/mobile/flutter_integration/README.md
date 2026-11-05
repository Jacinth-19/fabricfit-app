# Android/Flutter integration contract (not yet a working asset)

`assets/models/fabric_classifier.tflite` is intentionally absent. Training and
TFLite conversion have not completed, so do not wire a placeholder into the app.
Copy the model, `assets/labels/fabric_labels.txt`, and
`assets/config/model_config.json` into the Flutter project's asset tree only after
the release gate in the project README passes.

Expected inference contract from `train_model.py`:

- Resize to 224×224; RGB order, float input 0..255 (MobileNetV3 includes its
  [-1,1] scaling in the model graph). Do not separately normalize to [0,1].
- Output: 8 softmax values in the exact labels-file order.
- A tensor conversion must be verified against the generated TFLite input dtype
  and quantization scale/zero point; do not hardcode float if selecting INT8.
- Run asynchronously/off the UI thread; dispose the interpreter; handle camera
  orientation and crop identically to validation.
- Low confidence should say “Uncertain” rather than force a category. Calibrate
  the threshold on validation data and verify it on test data before product use.

Example once the model exists (Flutter `tflite_flutter`; preprocess bytes with a
verified RGB image library):

```dart
final interpreter = await Interpreter.fromAsset('models/fabric_classifier.tflite');
// input must be Float32List shaped [1, 224, 224, 3], RGB values 0..255.
final output = List.generate(1, (_) => List<double>.filled(8, 0));
interpreter.run(input, output);
final scores = output.single;
final best = scores.indexed.reduce((a, b) => a.$2 > b.$2 ? a : b);
if (best.$2 < calibratedThreshold) {
  return const ScanResult.uncertain();
}
return ScanResult(label: labels[best.$1], confidence: best.$2);
```

Before release compare predictions on identical test tensors between Keras and
TFLite, run on target Android hardware, record model size/latency, and keep the
app's demo/unvalidated disclosure until field evaluation supports a change.
