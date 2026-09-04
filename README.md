# Yomu

Yomu 是一个专注于本地图片的 Android 阅读器。

## 已实现

- 通过 Android 系统目录选择器挂载多个文件夹，访问权限可跨重启保留
- 递归扫描所有子文件夹，包括名称以 `.` 开头的隐藏文件夹
- 每个“直接包含图片”的目录自动成为一册
- JPG、PNG、WebP、GIF、BMP、HEIC/HEIF、AVIF 文件识别（实际解码能力取决于 Android 系统版本）
- 文件名自然排序，例如 `1.jpg`、`2.jpg`、`10.jpg`
- 自适应双列/多列书架、封面、搜索和阅读进度
- 分页阅读：滑动翻页、左右点按翻页、双击缩放、双指缩放和拖动
- 长图阅读：无缝纵向连续滚动
- 左到右 / 右到左阅读方向
- 沉浸式系统栏、页码滑杆、自动记忆每册进度
- 不申请“管理所有文件”权限，也不访问网络

## 打开与构建

1. 使用支持 Android Gradle Plugin 9.3 的 Android Studio 打开项目。
2. 安装 Android SDK 37.1，并让 Gradle 使用 JDK 17。
3. 等待 Gradle 同步后，运行 `app` 配置；或执行 `gradlew.bat assembleDebug`。

调试 APK 会生成在 `app/build/outputs/apk/debug/app-debug.apk`。

## 关于隐藏目录

扫描器不会过滤点号目录。能否在挂载时看见并选中某个隐藏目录，取决于手机自带的系统文件选择器；也可以挂载它的可见父目录，Yomu 会继续扫描内部的 `.hidden` 子目录。

Android 11 及以上不允许第三方应用通过目录选择器授权内部存储根目录、下载目录根、`Android/data` 或 `Android/obb`，这是系统限制。请选择它们下面允许访问的具体目录，或使用其他位置。

## 项目结构

- `data/LibraryRepository.kt`：目录授权、递归扫描、挂载与进度持久化
- `data/NaturalOrder.kt`：图片页码自然排序
- `ui/LibraryScreen.kt`：书架、搜索、目录管理
- `ui/ReaderScreen.kt`：分页与长图阅读器
- `ui/ImageLoader.kt`：本地 URI 解码、降采样和内存缓存
