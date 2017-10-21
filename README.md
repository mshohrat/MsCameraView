# MSCameraView

Basic useful feature list:

 * Taking pictures with device camera
 * Front and Back Camera
 * Auto focus on taking photos
 * Flash options on back camera
 * Easy to use
 
#Getting Started

add below line into your build.gradle file.
```java
    dependencies {
        compile 'com.github.mshohrat:MsCameraView:1.1.1'
    }
```

# How to use

For using library the only thing that you should do is creating an intent with specific Action and putting your intended file Uri as String to it, and then start for activity result the intent. after taking picture and verify taken picture, you can manage and use taken picture according to your code. Note that result contains rotation degree that photo must be rotate to and photo taken file Uri. See below Example.
```java
    // Send request to take picture
    File file = new File("example file");
    Intent intent = new Intent(CameraActivity.DEFAULT_ACTION);
    intent.putExtra(CameraActivity.EXTRA_OUTPUT, Uri.fromFile(file).toString());
    startActivityForResult(intent, 1001);
```

```java
    // Manage taking picture result
    if (requestCode == 1001 && resultCode == Activity.RESULT_OK){
          Uri photoTaken = null;
          int rotation = 0;
          if(data!=null) {
               rotation = data.getIntExtra(CameraActivity.RETURNED_IMAGE_ROTATION, rotation);
               photoTaken = data.getData();
          }
    
          if(photoTaken!=null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                AssetFileDescriptor fileDescriptor = null;
                try {
                    fileDescriptor = this.getContentResolver().openAssetFileDescriptor(selectedImage, "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
    
                if(fileDescriptor!=null) {
                    Bitmap actuallyUsableBitmap = BitmapFactory.decodeFileDescriptor(
                            fileDescriptor.getFileDescriptor(), null, options);
                    if(rotation!=0) {
                        actuallyUsableBitmap = CameraActivity.rotateBitmap(actuallyUsableBitmap, rotation);
                        if (actuallyUsableBitmap != null) {
                            //TODO do something with bitmap 
                        }
                    }
                }
    
        }
    }
```
also for more information you can see demo app. 

# Versioning
Text about versioning

## Authors
* Meysam Shohrat

## License
This project is licensed under the GNU General Public License - see the LICENSE.md file for details
