# 🏗️ 远端构建指南

本项目使用 **GitHub Actions** 实现自动化远端构建。每次推送代码到 GitHub 后，服务器会自动编译 APK。

## 🚀 快速开始

### 1. 推送代码到 GitHub

```bash
# 在项目根目录初始化 Git
git init
git add .
git commit -m "初始提交"

# 在 GitHub 创建仓库后关联
git remote add origin https://github.com/YOUR_USERNAME/VocabApp.git
git push -u origin main
```

### 2. 查看构建进度

1. 打开 GitHub 仓库页面
2. 点击 **Actions** 选项卡
3. 选择最新的 workflow 运行
4. 等待构建完成（约 3-5 分钟）

### 3. 下载 APK

构建完成后，在 Actions 运行详情页的底部 **Artifacts** 区域可以下载 APK 文件。

## 🔧 手动触发构建

1. 进入 GitHub 仓库 → **Actions** → **Build APK**
2. 点击 **Run workflow** 按钮
3. 选择构建类型（`debug` 或 `release`）
4. 点击 **Run** 开始构建

## 📱 安装 APK

下载后，将 APK 传输到 Android 设备，打开文件即可安装。

> ⚠️ 如果从浏览器下载，可能需要开启"允许安装未知来源应用"。

## 🔐 Release 签名构建

要构建正式发布的 Release APK，需要在 GitHub Secrets 中配置签名密钥：

| Secret 名称 | 说明 |
|-------------|------|
| `KEYSTORE_BASE64` | `.jks` 密钥库文件的 Base64 编码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |
| `STORE_PASSWORD` | 密钥库密码 |

### 配置步骤

1. **生成密钥库**（如已有可跳过）：
   ```bash
   keytool -genkey -v -keystore app/keystore.jks -alias vocabapp -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **编码为 Base64**：
   ```bash
   # Windows PowerShell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("app/keystore.jks"))
   
   # macOS / Linux
   base64 -i app/keystore.jks
   ```

3. **添加到 GitHub Secrets**：
   - 仓库 → **Settings** → **Secrets and variables** → **Actions**
   - 点击 **New repository secret**
   - 分别添加 `KEYSTORE_BASE64`、`KEY_ALIAS`、`KEY_PASSWORD`、`STORE_PASSWORD`

4. 创建 `app/keystore.properties`（本地调试用，不提交到 Git）：
   ```properties
   storeFile=keystore.jks
   storePassword=your_password
   keyAlias=vocabapp
   keyPassword=your_key_password
   ```

## 🧪 本地构建测试

```bash
# 确保已安装 JDK 17 和 Android Studio

# 生成 Gradle Wrapper（首次）
gradle wrapper --gradle-version 8.5

# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

## 📋 构建产物

| 构建类型 | 输出路径 |
|----------|----------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` |

## 🔄 工作流说明

工作流文件位于 `.github/workflows/build.yml`，主要步骤：

1. **检出代码** - 获取最新代码
2. **设置 JDK 17** - 配置 Java 环境
3. **设置 Android SDK** - 配置 SDK 和构建工具
4. **生成 Gradle Wrapper** - 自动创建 `gradlew` 脚本
5. **缓存依赖** - 加速后续构建
6. **编译 APK** - 执行 `assembleDebug` 或 `assembleRelease`
7. **上传产物** - 将 APK 保存为构建工件（保留 30 天）
