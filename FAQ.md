Q & A
-------

#### 1. Camera preview black screen?

**First**, check `targetSdk` version. If `targetSdk>=28`, you should grant the app `android.permission.CAMERA` permission  and this solution will be resolved as usual. However, if it doesn't work, you have to set your targetSdk below 28 because it's custom system problem. **Otherwise**,  issues to me and send me a email with your logs at location `Android/data/com.jiangdg.ausbc/files` if you have already get the uvc camera permission greanted dialog.

