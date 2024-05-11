# oxyzen-android-sample

```shell
cd example
./download_aar.sh # 下载OxyZenSDK AAR文件
```

```java
// disable log
ZenLiteSDK.setLogLevel(ZenLiteSDK.LogLevel.NONE);
ZenLiteSDK.setLogCallback(null);

// set log level
ZenLiteSDK.setLogLevel(ZenLiteSDK.LogLevel.ERROR);

// if you want to record and debug, you can use setLogCallback and print or save log message
ZenLiteSDK.setLogCallback(new OnLogCallback()  {
    public void invoke(String message) {
        // TODO: saveLogMessage(message);
    }
});
```
