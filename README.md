# Limbo PC Emulator Extended

<p align="center">
  <b>A modernized, high-performance QEMU-based PC emulator for Android</b><br>
  Rebuilt with SDL3, full 16KB Page Size support (Android 15 / Galaxy S25 Ultra), and an ergonomic Hacker's-style virtual keyboard & mouse drag-and-drop system.
</p>

---

## 🌟 概要 (Overview)

**Limbo PC Emulator Extended** は、名作 Android 向け PC エミュレータである [Limbo Emulator](https://github.com/limboemu/limbo) をベースに、最新の Android 環境および近年のフラッグシップ端末に合わせてアーキテクチャ全体を刷新・進化させた拡張版エミュレータです。

オリジナルの Limbo（v6.0.1 / v7.0.0-alpha）で長年課題となっていた**「画面回転やアプリ切り替え時のブラックアウト（黒画面クラッシュ）」**や、最新の Android 15 / 16KB ページサイズ端末での動作不能問題を根本解消し、さらに実用的な PC 操作を可能にする**「Hacker's Keyboard 風ビルトインキーボード」**と**「L-Click / R-Click ホールドドラッグ（右クリックドラッグ対応）」**を搭載しました。

---

## 🚀 オリジナル版との比較・進化点 (What's New & Evolution)

| 機能・項目 | オリジナル版 (limboemu/limbo) | Limbo Extended (本作) |
| :--- | :--- | :--- |
| **GUI コア** | レガシー SDL2 + 独自拡張パッチ | **最新 SDL3 & sdl2-compat による近代化アーキテクチャ** |
| **画面ブラックアウト** | 画面回転やバックグラウンド復帰時に高確率で黒画面クラッシュ | **サーフェスライフサイクルの完全刷新により根本解消** |
| **最新 Android 対応** | Android 15 / 16KB ページサイズ端末（Galaxy S25 Ultra 等）でクラッシュ | **16KB ELF アライメント (max-page-size=16384) に完全対応** |
| **仮想キーボード** | ボタンが小さく、入力時にエミュレータ画面下部が覆い隠される | **Hacker's Keyboard 風フルキーボード（常時展開・ハイライト表示・Fn切替・テンキー・F1-F12完備）** |
| **画面レイアウト** | キーボードを出すと画面と被って操作困難 | **SDL 画面をツールバー直下に固定配置し、キーボードと一切干渉しない最適レイアウト** |
| **マウスクイックバー** | 単純なタップ・クリックのみ | **L-Click, Wheel (UP/DOWN/スワイプ), R-Click のクイックバー搭載** |
| **マウスホールドドラッグ** | 不安定、またはドラッグ中にボタンが解放されてしまう | **L-Click / R-Click の長押しでホールド状態（L-HOLD / R-HOLD）に移行。ウィンドウ移動や右ドラッグに完全対応** |
| **ドロップ・メニュー操作** | ドロップ時に誤動作しやすい | **ホールド解除時の二重 UP 送信防止ロジックにより、右ドラッグメニュー等の誤消去を防止** |
| **メディアマウント** | CD-ROM (.iso) の動的挿入・排出時にクラッシュやファイルロック | **安全なファイルオープン・ロック解除処理によりクラッシュ防止** |
| **画面回転設定** | 横画面（SENSOR_LANDSCAPE）に固定されがち | **OS の画面自動回転・縦固定・横固定設定にスムーズに追従** |

---

## ⌨️ 操作方法ガイド (Controls & Ergonomics)

### 1. マウスクイックバー & ホールドドラッグ
キーボード上部のバーにマウス操作用のボタンが配置されています。

- **L-Click (通常クリック)**:
  - 短くタップすると左クリック（シングルクリック）が発行されます。
- **L-HOLD (左クリックホールド / 左ドラッグ)**:
  - `L-Click` を **長押し（約0.4秒）** すると、端末が振動してボタンが青く光り `L-HOLD` に変化します。
  - そのまま画面上を指でなぞると、ウィンドウの移動や範囲選択などの**左ボタンドラッグ**が行えます。
  - 目的の位置まで移動したら、再度 `L-HOLD` ボタンをタップするとホールドが解除（ドロップ）されます。
- **R-Click (右クリック)**:
  - 短くタップすると右クリックが発行され、コンテキストメニューが開きます。
- **R-HOLD (右クリックホールド / 右ドラッグ)**:
  - `R-Click` を **長押し（約0.4秒）** すると、`R-HOLD` に変化します。
  - ファイルやオブジェクト上でホールドした後、画面をなぞって別のフォルダやデスクトップへ移動できます。
  - 移動先で再度 `R-HOLD` ボタンをタップして解除すると、Windows 等で**「ここに移動」「ここにコピー」**の右ドラッグメニューが表示されます。
- **ホイール操作 (Wheel)**:
  - `▲` / `▼` ボタンをタップして素早くスクロールできます。
  - 中央の `[ WHEEL ]` 領域を指で上下にスワイプすることによる連続スクロールにも対応しています。

### 2. Hacker's Keyboard 風ビルトインキーボード
- **押下ハイライト**: 各キーをタップした際に視覚的にハイライト表示され、確実なタイピングを支援します。
- **修飾キーのラッチ**: `Shift`、`Ctrl`、`Alt`、`Win` キーはタップするとトグル（ラッチ）固定され、ショートカットキー（例: Ctrl + C, Alt + F4）の入力が簡単に行えます。
- **Fn キー切替**:
  - `Fn` キーをタップすることで、メインキーボードとサブキーボード（F1〜F12、Insert、Delete、Home、End、PageUp/Down、および独立テンキー）がワンタップで切り替わります。
- **非表示ボタン**:
  - 右上の `[ - ]` ボタンをタップすることで、キーボードを最小化して画面全体を広く使うことができます。

---

## 📦 ダウンロード (Downloads)

最新のビルド済み APK は [GitHub Releases](https://github.com/Yasai-Douhu/Limbo-PC-Emulator-Extended/releases) よりダウンロードいただけます。

- **推奨環境**: Android 8.0 以上（Android 14 / 15、Galaxy S25 Ultra 等の 16KB ページサイズ環境を公式サポート）
- **対象アーキテクチャ**: arm64-v8a, x86_64, armeabi-v7a, x86

---

## 🛠️ ビルド方法 (Building from Source)

### 必要要件
- Android NDK r23b 以上
- Android SDK (API Level 34, Build Tools 34.0.0)
- Gradle 6.7.1 または 8.x
- Clang (16KB Page Size サポート: `-Wl,-z,max-page-size=16384`)

### ネイティブライブラリのビルド
```bash
# SDL3 / sdl2-compat 連携ライブラリのビルド
cd limbo-android-lib/src/main/jni/compat/sdl-extensions
$CLANG -fPIC -shared -O2 \
    -Wl,-z,max-page-size=16384 \
    -I. -I$SDL2_INC \
    SDL_limbomouse.c SDL_limboscreen.c \
    -L$LIBS_DIR -lSDL2 -llog \
    -o libcompat-SDL2-ext.so
```

### APK のビルド
```bash
./gradlew :limbo-android-x86:assembleRelease
```

---

## 📜 ライセンス & 謝辞 (License & Acknowledgements)

- **License**: GNU General Public License v2 (GPL-2.0)
- **Original Limbo PC Emulator**: Copyright (C) Max Kastanas / [limboemu](https://github.com/limboemu/limbo)
- **QEMU**: [QEMU Team](https://www.qemu.org/)
- **SDL**: [Simple DirectMedia Layer (SDL3)](https://www.libsdl.org/)
