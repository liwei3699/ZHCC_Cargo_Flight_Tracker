# 郑州货运航班动态（非官方）

Android 13+ 客户端，读取 [郑州机场货运站](https://www.zzcargo.com/airline/1) 的公开「航班动态」数据。

## 功能

- 全量抓取当前航班动态（约 600+ 条）
- 航班号 / 航班日期 / 进出港 / 代理 / 航司 / 机型 / 始发站 / 目的站筛选
- 全局关键词搜索、到达 / 起飞 / 延误快捷筛选
- 按起降机场、计划/实际起降时间、航班日期、机型、航班号排序，支持升序/降序
- 航司名称来自官网航司目录，常见货运代码有内置中文名兜底
- 前台每 30 分钟自动刷新，手动刷新 + 下拉刷新
- 卡片列表与完整详情；本地 JSON 缓存，断网可看上次数据
- 导出当前筛选结果为 19 列 xlsx 到下载目录，支持打开 / 分享

## 下载与安装

### 正式版（推荐）

从本仓库的 GitHub Releases 页面下载 `郑州货运航班动态-<version>-release.apk`；本地已构建版本位于
`dist/郑州货运航班动态-1.1.0-release.apk`（该目录不提交到 git）。

- 手机需为 Android 13 及以上。
- 正式版与开发调试版签名不同，若手机已安装 debug 版，请先卸载再安装正式版。
- 首次启动会抓取全部分页，保持联网即可。

### 开发调试版

```powershell
.\gradlew.bat :app:assembleDebug
```

输出 `app/build/outputs/apk/debug/app-debug.apk`。

## 构建

### 环境

- JDK 17
- Android SDK platform 35、build-tools 35.0.0
- Gradle 8.9（仓库自带 wrapper）

在项目根目录创建 `local.properties`：

```properties
sdk.dir=C:/Users/<你的用户名>/AppData/Local/Android/Sdk
```

大陆网络如果 Gradle 官方分发下载缓慢，可临时把 `gradle/wrapper/gradle-wrapper.properties`
的 `distributionUrl` 改为腾讯镜像；`distributionSha256Sum` 无需修改。

### Release 构建

1. 复制示例配置并填入本地签名信息：

   ```powershell
   Copy-Item keystore.properties.example keystore.properties
   ```

   `keystore.properties` 中的 `storeFile` 指向本地 keystore，密码只保存在本机。

2. 将正式 keystore 放在 `keystore/zzcargo-release.jks`（该目录已被 gitignore）。

3. 构建并签名：

   ```powershell
   .\gradlew.bat :app:assembleRelease
   ```

4. 输出：`app/build/outputs/apk/release/app-release.apk`

Release 已启用 R8 混淆、资源压缩和 release 签名。**不要提交** `keystore.properties`、
`keystore/`、`.jks` 或任何密码文件。

## 测试

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

当前共 59 项单元测试，覆盖解析、筛选、排序、缓存、xlsx、ViewModel 和航司目录。

## GitHub Actions 发布

仓库包含 `.github/workflows/release.yml`：推送 `v*` 标签时自动跑测试、构建签名 release APK，
并创建 GitHub Release 附件。

在 GitHub 仓库的 **Settings → Secrets and variables → Actions** 添加：

| Secret | 值 |
|---|---|
| `KEYSTORE_BASE64` | 本地 keystore 的 base64 内容 |
| `KEYSTORE_PASSWORD` | `keystore.properties` 中的 `storePassword` |
| `KEY_ALIAS` | `zzcargo` |
| `KEY_PASSWORD` | `keystore.properties` 中的 `keyPassword` |

生成 `KEYSTORE_BASE64`（PowerShell）：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore\zzcargo-release.jks")) |
    Set-Content keystore\release.jks.base64 -NoNewline
```

然后：

```powershell
git tag v1.1.0
git push origin v1.1.0
```

工作流会生成 `郑州货运航班动态-v1.1.0-release.apk` 并附加到 Release。密钥或 Secrets
缺失时工作流会失败，这是预期行为。

## 签名密钥备份

正式签名文件、密码和证书指纹保存在本地 `keystore/` 与 `keystore.properties`，均已加入
`.gitignore`，不会进入 GitHub。请立即离线备份这两项：

- `keystore/zzcargo-release.jks`
- `keystore/CREDENTIALS.txt`（包含密码和证书 SHA-256）

丢失密钥后，无法为已发布应用提供可升级的新版本。不要把密码、keystore 或 base64 内容
提交到公开仓库或发送到公开渠道。

## 导出位置

`下载/郑州机场航班动态/郑州机场货运航班动态_yyyyMMdd_HHmmss.xlsx`

## TLS/证书说明

zzcargo.com 当前只下发叶子证书，Android 无法自动补齐 GDCA 中间证书。应用内置
GDCA TrustAUTH R4 OV SSL CA2 中间证书与 GDCA TrustAUTH R5 ROOT 根证书，仅用于该站点的
严格链式校验；系统信任校验仍优先。站点更换 CA 或中间证书后需要更新 APK。

## 数据来源与免责声明

- 仅申请网络权限；不收集、不上传用户数据。
- 数据来自公开网页接口，本应用为非官方工具，与河南省机场集团/郑州机场货运站无关。
- 网站改版可能导致抓取失败，此时保留上次缓存并提示。
- 不在后台刷新，不发送通知，不包含「航班计划」页面。

## 许可证

[MIT](LICENSE)
