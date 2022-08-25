Q & A
-------

#### 1. Camera preview black screen?

**First**, check `targetSdk` version. If `targetSdk>=28`, you should grant the app `android.permission.CAMERA` permission  and this solution will be resolved as usual. However, if it doesn't work, you have to set your targetSdk below 28 because it's custom system problem. **Otherwise**,  issues to me and send me a email with your logs at location `Android/data/com.jiangdg.ausbc/files` if you have already get the uvc camera permission greanted dialog.

#### 2. How to filter some device？

If your uvc device can not be recognised, maybe you can try like this:

**First**, create a flie named `default_device_filter.xml` in app xml directory(If not exist, please create it.).

**then**, copying the default value into it and adding your uvc device information.

```xml
<?xml version="1.0" encoding="utf-8"?>
<usb>
	<usb-device class="239" subclass="2" />	<!-- all device of UVC -->
	<!-- a few android 9.0 -->
	<usb-device class="14" subclass="9" />
	<usb-device class="2" subclass="0" />
	<usb-device class="6" subclass="-1" />
	<usb-device class="39" subclass="0" />
	<usb-device product-id="4836" vendor-id="9573" />
	<usb-device product-id="2229" vendor-id="1133" />
	<usb-device product-id="640" vendor-id="1409" />
	<usb-device product-id="258" vendor-id="9228" />
	<usb-device product-id="16981" vendor-id="19" />
    <!-- your uvc device -->
    <usb-device product-id="xxx" vendor-id="xxx" />
    <usb-device product-id="xxx" vendor-id="xxx" />
</usb>
```

#### 3. There is a certain probability of being ANR when operatoring hot plug?

Please update to `3.2.7`+ and this version has been optimized accordingly.