---
type: build-instructions
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# ビルド手順 — keyword-search

## 前提条件

| 項目 | 内容 |
|---|---|
| Java | 25（`java -version` で確認） |
| Gradle | ラッパー使用（`./gradlew`） |
| Node.js | 18+ |
| PostgreSQL | ローカル起動 or Docker（バックエンド統合テスト用） |

---

## バックエンドビルド

```bash
cd /workspace/backend

# 依存関係の解決（jqwik 1.9.1 含む）
./gradlew dependencies

# コンパイル + Spotless フォーマット + Checkstyle
./gradlew spotlessApply build -x test
```

### 期待される出力

```
BUILD SUCCESSFUL in Xs
```

### よくあるエラー

| エラー | 原因 | 対処 |
|---|---|---|
| `Could not resolve net.jqwik:jqwik:1.9.1` | Maven Central への接続失敗 | ネットワーク確認・Gradle キャッシュクリア（`./gradlew --refresh-dependencies`）|
| Checkstyle エラー | Google Java Format 違反 | `./gradlew spotlessApply` を先に実行 |
| JPQL 構文エラー | `findWithFilters` の JPQL に誤り | クエリの括弧・パラメータ名を確認 |

---

## フロントエンドビルド

```bash
cd /workspace/frontend

# 依存関係インストール
npm install

# 型チェック
npm run type-check 2>/dev/null || npx tsc --noEmit

# ビルド
npm run build
```

### 期待される出力

```
Route (app)                              Size     First Load JS
┌ ○ /resources                          ...
```
