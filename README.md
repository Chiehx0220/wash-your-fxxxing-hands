# 抓！沒洗手

三總松山分院 手部衛生遵從率稽核系統注入 Material 3 Expressive 樣式與快速填寫功能

## 建置

需要 Android Studio 或 JDK 17 與 Android SDK。

```bash
./gradlew installDebug
```

## 名單

`app/src/main/assets/roster.json`：科別對應姓名陣列。含個人資料，已列入 `.gitignore` 不進版控；本機放檔案再 build 即可，缺檔時名單功能停用（CI 與 release 版沒有名單）。

```json
{ "骨科": ["姓名一", "姓名二"], "小兒科": [] }
```

## 結構

- `MainActivity.kt`：WebView、網址白名單、`addDocumentStartJavaScript` 注入
- `NativeStore.kt`：JS 橋接（偏好設定、名單、主題 CSS、開啟 repo）
- `ThemeCss.kt` / `Palette.kt`：動態色彩 → `--m3-*` 變數
- `SplashView.kt` / `ExpressiveViews.kt`：啟動畫面與形狀變形動畫
- `assets/inject.js` / `theme.css`：表單增強與樣式
