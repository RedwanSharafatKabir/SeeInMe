## Android Product Detector App using TensorFlow Lite image classification

### Overview

This is an example application for [TensorFlow Lite](https://tensorflow.org/lite)
on Android. It uses
[Image classification](https://www.tensorflow.org/lite/models/image_classification/overview)
to continuously classify whatever it sees from the device's back camera.
Inference is performed using the TensorFlow Lite Java API. The demo app
classifies frames in real-time, displaying the top most probable
classifications. It allows the user to choose between a floating point or
[quantized](https://www.tensorflow.org/lite/performance/post_training_quantization)
model, select the thread count, and decide whether to run on CPU, GPU, or via
[NNAPI](https://developer.android.com/ndk/guides/neuralnetworks).

These instructions walk you through building and
running the demo on an Android device. For an explanation of the source, see
[TensorFlow Lite Android image classification example](https://www.tensorflow.org/lite/models/image_classification/android).

<!-- TODO(b/124116863): Add app screenshot. -->
<br>

### Model
Inside Assests folder zip file is there.

Resnet50 
16 batch size
100 epochs
Teachable ML
<br><br>

### Screen Shots

#### Detect Product -
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;    ![product1](https://user-images.githubusercontent.com/37416018/142223670-d47a6c5c-c51b-4727-873a-f138e14ac2b3.jpg)
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;    ![product2](https://user-images.githubusercontent.com/37416018/142223680-5f978be8-78e4-4bc5-8bf1-edd19574f8c9.jpg)
- - - -

#### Voice Command -
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;    ![voice command](https://user-images.githubusercontent.com/37416018/142223655-71774b00-1cf2-48ea-96a9-99247e14e9e6.jpg)
- - - -

#### Cart List - 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;    ![cart](https://user-images.githubusercontent.com/37416018/142223662-9bf8ce27-e4d6-43da-950d-a592b02ea89e.jpg)
- - - -
<br>

### Requirements

*   Minimum Android Studio 3.2 and Recomended Artic Fox | 2020.3.1

*   Android device in
    [developer mode](https://developer.android.com/studio/debug/dev-options)
    with USB debugging enabled

*   USB cable (to connect Android device to your computer)
<br>
