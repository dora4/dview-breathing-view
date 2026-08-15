dview-breathing-view ![Release](https://jitpack.io/v/dora4/dview-breathing-view.svg)
--------------------------------

#### Gradle依赖配置

```groovy
// 添加以下代码到项目根目录下的build.gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
// 添加以下代码到app模块的build.gradle
dependencies {
    implementation 'com.github.dora4:dview-breathing-view:1.2'
}
```

#### 控件使用

```xml
<dora.widget.DoraBreathingView
    android:id="@+id/breathingView"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dview_bv_text="PUSH START"
    app:dview_bv_textSize="12sp"
    app:dview_bv_blinkDuration="1000"/>
```

```kotlin
val breathingView = headerView.findViewById<DoraBreathingView>(R.id.breathingView)
breathingView.blink(10)
```

