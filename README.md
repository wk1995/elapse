# Raster  anr 诊断

ANR(Application Not Responding)应用程序无响应是Android中最常见的性能问题，也是难以追踪和解决的老大难问题，
一旦出现ANR, 对用户体验造成很大的影响。data/anr只能记录的是anr发生的当前时间点各个进程/线程当前堆栈，这往往并
不能帮助发现ANR的具体原因。

Raster是为了解决ANR问题而生，它是基于Android的Handler机制，记录和回溯主线程消息队列。可以输出慢消息执行时长、调用堆栈
等关键信息。能够发现发生ANR具体细节。


## 接入方式

1. 添加raster依赖

```
// build.gradle

...
depenedencies {
    ...
    implementation 'com.tuya.smart:raster:0.0.1-SNAPSHOT'
    ...
}

```

2. 在Application的onCreate方法中添加raster初始化

```
public void onCreate() {
    RasterOptions options = new RasterOptions(); // 更多参数详情参考 RasterOptions.kt
    options.slowThreshold = 300L;       // 单条记录最大时长, 连续多条消息时间总和在recordMaxDuration内的，会聚合记录在一条Msgs记录
    options.recordMaxDuration = 400;    // raster只记录最近timeLineDuration时间内的记录，超过timeLineDuration的记录会被清除
    Raster.init(this, options);
}
```

3. 监听ANR发生时，dump出主线程消息记录

```
TuyaCrash.registerAnrCallback() {
    Raster.dump();
}
```

4. dump文件位置在: /sdcard/Android/data/<packageName>/files/raster/raster-dump-xxxxx.txt
