---
type: nfr-design
artifact: nfr-design-patterns
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# NFR 設計パターン — keyword-search

## パターン 1: 安全な JPQL パラメータバインド（SECURITY-05）

### 目的
`keyword` の部分一致検索を SQL インジェクションなしに実装する。

### パターン
**Named Parameter Binding（名前付きパラメータバインド）**

```java
// ResourceRepository.java
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
```

### 制約（ハードコンストレイント）

- `:keyword` は JPA が PreparedStatement のバインドパラメータとして扱う → インジェクション不可
- `CONCAT('%', :keyword, '%')` は JPQL の文字列関数として安全に評価される
- **禁止**: `"... LIKE '%" + keyword + "%'"` のような文字列連結

### Service 層での keyword 正規化

```java
// ResourceService.list() 内
String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
```

---

## パターン 2: Java Stream 安全フィルタ（listWithAvailabilityFilter パス）

### 目的
`from`/`to` 指定時の候補リストに対し、Java で安全にキーワードフィルタを適用する。

### パターン

```java
// ResourceService.listWithAvailabilityFilter() 内
List<Resource> candidates = fetchAllCandidates(category, isAdmin);

// keyword フィルタ（SQL 非関与のため injection リスクなし）
if (normalizedKeyword != null) {
    String lowerKw = normalizedKeyword.toLowerCase(Locale.ROOT);
    candidates = candidates.stream()
        .filter(r -> r.getName().toLowerCase(Locale.ROOT).contains(lowerKw)
                  || (r.getDescription() != null
                      && r.getDescription().toLowerCase(Locale.ROOT).contains(lowerKw)))
        .toList();
}
```

**注**: `Locale.ROOT` を使用して locale 依存の大文字変換を避ける（トルコ語の 'i'/'I' 問題等）。

---

## パターン 3: プロパティベーステスト構造（PBT-07/08/09）

### 目的
jqwik を使って keyword フィルタの不変条件を自動検証する。

### ジェネレータ設計（PBT-07 準拠）

```java
// テストヘルパー: Resource ビルダー
@Provide
Arbitrary<Resource> resources() {
    Arbitrary<String> names = Arbitraries.strings()
        .withCharRange('a', 'z')
        .ofMinLength(1).ofMaxLength(50);
    Arbitrary<String> descriptions = Arbitraries.strings()
        .withCharRange('a', 'z')
        .ofMinLength(0).ofMaxLength(200)
        .injectNull(0.2);  // 20% の確率で null（description は nullable）
    return Combinators.combine(names, descriptions)
        .as((name, desc) -> buildResource(name, desc));
}

@Provide
Arbitrary<String> keywords() {
    return Arbitraries.strings()
        .withCharRange('a', 'z')
        .ofMinLength(1).ofMaxLength(20);
}
```

### プロパティテスト構造（P-01〜P-05）

```java
// P-01: マッチ結果の完全性（全結果が keyword を含む）
@Property
void allResultsContainKeyword(
    @ForAll("resources") List<Resource> resources,
    @ForAll("keywords") String keyword) {
    List<Resource> result = applyKeywordFilter(resources, keyword);
    result.forEach(r ->
        assertThat(
            r.getName().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))
            || (r.getDescription() != null
                && r.getDescription().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)))
        ).isTrue()
    );
}

// P-02: 単調性（絞り込み結果 <= 全件）
@Property
void keywordFilterReducesOrMaintainsSize(
    @ForAll("resources") List<Resource> resources,
    @ForAll("keywords") String keyword) {
    assertThat(applyKeywordFilter(resources, keyword).size())
        .isLessThanOrEqualTo(resources.size());
}

// P-03: 冪等性
@Property
void filterIsIdempotent(
    @ForAll("resources") List<Resource> resources,
    @ForAll("keywords") String keyword) {
    List<Resource> once = applyKeywordFilter(resources, keyword);
    List<Resource> twice = applyKeywordFilter(once, keyword);
    assertThat(twice).isEqualTo(once);
}

// P-04: null keyword で全件返却
@Property
void nullKeywordReturnsAll(@ForAll("resources") List<Resource> resources) {
    assertThat(applyKeywordFilter(resources, null)).isEqualTo(resources);
}

// P-05: description=null のリソースは name のみでマッチ判定
@Property
void nullDescriptionResourceMatchesOnNameOnly(
    @ForAll @StringLength(min=1, max=20) String keyword) {
    Resource r = buildResource("nomatch", null);  // description=null
    assertThat(applyKeywordFilter(List.of(r), keyword)).isEmpty();
}
```

### shrinking・再現性（PBT-08 準拠）

- jqwik はデフォルトで shrinking が有効（無効化しない）
- 失敗時は seed が標準出力に表示される（jqwik デフォルト）
- CI では `@Property(seed=...)` を使わず毎回異なる seed で実行（失敗時に seed を確認して再現可能）

---

## セキュリティパターン適合サマリー

| SECURITY ルール | パターン | 実装箇所 |
|---|---|---|
| SECURITY-05 | Named Parameter Binding | `ResourceRepository.findWithFilters()` |
| SECURITY-08 | 既存 JWT Guard 維持 | `ResourceController.list()` — 変更なし |
| SECURITY-10 | jqwik 1.9.1 固定バージョン | `build.gradle.kts` |
