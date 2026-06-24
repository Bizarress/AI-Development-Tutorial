---
type: nfr-design
artifact: logical-components
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# 論理コンポーネント — keyword-search

## 新規論理コンポーネント

本エンハンスは**既存コンポーネント境界内の拡張**のため、新規インフラコンポーネント（キュー・キャッシュ・サーキットブレーカー等）は追加しない。

テストスコープでのみ jqwik フレームワークを追加する（実行時コンポーネントではない）。

---

## 変更される既存コンポーネント

```
+---------------------+          +---------------------+
|  ResourceController |          |  ResourceFilterForm  |
|  (Spring MVC)       |          |  (React Client Comp) |
|                     |          |                      |
|  + keyword param    |          |  + keyword input     |
|    @RequestParam    |          |  + handleSubmit 更新  |
+----------+----------+          +----------+-----------+
           |                                |
           | keyword                        | ?keyword=...
           v                                v
+----------+----------+          +----------+-----------+
|  ResourceService    |          |  Next.js page.tsx    |
|  (Spring @Service)  |          |  (Server Component)  |
|                     |          |                      |
|  + normalize(kw)    |          |  + searchParams.kw   |
|  + list(kw)         |          |    → ResourceFilter  |
|  + listPaginated(kw)|          |      Form props      |
|  + listWithAvail(kw)|          +---------------------+
+----------+----------+
           |
    +------+------+
    |             |
    v             v
+---+---+   +----+----+
| DB    |   | Java    |
| level |   | Stream  |
| JPQL  |   | filter  |
+---+---+   +----+----+
    |
    v
+---+---------+
| ResourceRepo|
| (JPA Repo)  |
|             |
| + findWith  |
|   Filters() |
| + findAll   |
|   Candidates|
+-------------+
```

---

## テストコンポーネント（テストスコープのみ）

| コンポーネント | 役割 | スコープ |
|---|---|---|
| `jqwik 1.9.1` | プロパティベーステストフレームワーク | `testImplementation` |
| `ResourceServiceTest`（拡張） | keyword フィルタの例ベーステスト + PBT | テスト |
| `ResourceControllerTest`（拡張） | keyword パラメータの Controller レベルテスト | テスト |

---

## 依存関係の変更

### backend/build.gradle.kts

```kotlin
// 追加（testImplementation セクション）
testImplementation("net.jqwik:jqwik:1.9.1")
```

**影響**: テストのみ。本番ビルド（jar）には含まれない。

---

## 論理コンポーネント非採用の判断

| 候補 | 採用しない理由 |
|---|---|
| 検索インデックス（pg_trgm） | スコープ外。リソース数が限定的な学習環境では不要 |
| キャッシュ（Redis 等） | スコープ外。検索結果のキャッシュは本エンハンスの要件にない |
| 非同期処理 | スコープ外。同期 HTTP リクエスト-レスポンスで十分 |
| サーキットブレーカー | スコープ外。外部サービス呼び出しなし |
