---
type: functional-design
artifact: business-logic-model
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# ビジネスロジックモデル — keyword-search

## 1. キーワード検索の全体フロー

```
HTTP GET /api/resources?keyword=会議室&category=ROOM&from=...&to=...
           |
           v
[ResourceController.list()]
  - @RequestParam(required=false) String keyword を受け取る
  - from/to のバリデーション（既存）
  - isAdmin 判定（既存）
  - keyword を ResourceService.list() に渡す
           |
           v
[ResourceService.list()]
  - keyword の正規化: blank(null or empty) → null に統一
  - from/to の有無で分岐:
      |
      +-- from/to なし --> listPaginated(category, isAdmin, keyword, pageable)
      |                        |
      |                        v
      |                   [DB レベルフィルタ]
      |                   ResourceRepository の @Query で
      |                   JPQL LOWER+LIKE による絞り込み
      |
      +-- from/to あり --> listWithAvailabilityFilter(category, from, to, isAdmin, keyword, pageable)
                               |
                               v
                          [Java Stream フィルタ]
                          fetchAllCandidates() で全件取得後、
                          空き確認フィルタ → keyword フィルタの順に適用
```

## 2. キーワード正規化ロジック（ResourceService）

```
normalizeKeyword(String raw):
  if raw == null OR raw.isBlank()  →  return null   // フィルタなし
  else                              →  return raw    // そのまま渡す（trim は任意）
```

**決定**: 長さバリデーションなし（Q1=B）。null/blank のみ「フィルタなし」として扱う。

## 3. DB レベルフィルタ（listPaginated パス）

### 3.1 JPQL クエリ設計

keyword 条件を `@Query` メソッドとして Repository に追加する。

**条件式**（JPQL）:
```sql
(:keyword IS NULL
 OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
 OR (r.description IS NOT NULL
     AND LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))))
```

**パラメータバインド**: `:keyword` は JPQL の名前付きパラメータ（`@Param("keyword")`）でバインド。文字列連結は使用しない（SECURITY-05 準拠）。

### 3.2 追加 Repository メソッドのシグネチャ（案）

| 既存メソッド | keyword 追加版 |
|---|---|
| `findByIsActiveTrue(Pageable)` | `findByIsActiveTrueAndKeyword(keyword, Pageable)` → `@Query` |
| `findByCategoryAndIsActiveTrue(cat, Pageable)` | keyword 込みの `@Query` |
| `findAll(Pageable)` (ADMIN) | keyword 込みの `@Query` |
| `findByCategory(cat, Pageable)` (ADMIN) | keyword 込みの `@Query` |

**簡略化案**: keyword あり/なしで分岐するより、単一 `@Query` に `(:keyword IS NULL OR ...)` を組み込む方が Repository の爆発を防ぐ。

→ 推奨: **category・isActive・keyword を束ねた単一の `@Query` メソッドを2本追加する（ページネーション用・全件用）**。

```java
// ページネーション用（listPaginated で使用）
@Query("SELECT r FROM Resource r " +
       "WHERE (:isAdmin = true OR r.isActive = true) " +
       "AND (:category IS NULL OR r.category = :category) " +
       "AND (:keyword IS NULL " +
       "     OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
       "     OR (r.description IS NOT NULL " +
       "         AND LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
Page<Resource> findWithFilters(
    @Param("isAdmin") boolean isAdmin,
    @Param("category") ResourceCategory category,
    @Param("keyword") String keyword,
    Pageable pageable);

// 全件取得用（listWithAvailabilityFilter の fetchAllCandidates で使用）
@Query("SELECT r FROM Resource r " +
       "WHERE (:isAdmin = true OR r.isActive = true) " +
       "AND (:category IS NULL OR r.category = :category)")
List<Resource> findAllCandidates(
    @Param("isAdmin") boolean isAdmin,
    @Param("category") ResourceCategory category);
```

> **注**: `findAllCandidates` は keyword フィルタを含まない（Java Stream で行うため）。

## 4. Java Stream フィルタ（listWithAvailabilityFilter パス）

```java
// fetchAllCandidates() で全件取得（keyword なし）
List<Resource> candidates = fetchAllCandidates(category, isAdmin);

// keyword フィルタ（keyword != null のとき適用）
if (keyword != null) {
    String lowerKeyword = keyword.toLowerCase();
    candidates = candidates.stream()
        .filter(r ->
            r.getName().toLowerCase().contains(lowerKeyword)
            || (r.getDescription() != null
                && r.getDescription().toLowerCase().contains(lowerKeyword)))
        .toList();
}

// 空き確認フィルタ（既存ロジック）
// ...（変更なし）
```

**適用順序**: keyword フィルタ → 空き確認フィルタ（どちらが先でも結果は同じだが、keyword で件数を先に絞る方が空き確認の候補 ID 取得クエリを小さくできる）

## 5. テスタブルプロパティ（PBT-01 — 必須）

| # | プロパティカテゴリ | 対象 | 検証内容 |
|---|---|---|---|
| P-01 | **Invariant** | keyword フィルタ結果 | 結果の全 Resource が `name` または `description`（大文字小文字無視）に keyword を含む |
| P-02 | **Invariant** | 単調性 | `filter(list, keyword).size() <= filter(list, null).size()` |
| P-03 | **Idempotence** | フィルタの冪等性 | `filter(filter(list, kw), kw) == filter(list, kw)` |
| P-04 | **Invariant** | null-safe | `keyword = null` のとき、全件が返される（フィルタなし） |
| P-05 | **Invariant** | description null-safe | `description = null` のリソースは keyword にマッチしない |

これらは `ResourceServiceTest` にて jqwik で検証する（NFR Requirements / Code Generation フェーズで詳細化）。
