---
type: nfr-design
title: NFR デザインパターン — リソース一覧のソート順選択
unit: resource-list-sort
timestamp: 2026-06-24T10:45:00Z
---

# NFR デザインパターン — resource-list-sort

## 1. セキュリティパターン（SECURITY-05）

### パターン: 入力許可リスト検証（Allowlist Validation Pattern）

**適用箇所**: `ResourceService.parseSortParam()`

```
HTTP Request
  ?sort=field,direction
       |
       v
[ResourceController]
  Pageable pageable + String sort (raw)
       |
       v
[ResourceService.parseSortParam(sort)]  <-- SECURITY-05 検証ここで実施
  1. sort が null/空 → DEFAULT ("createdAt", ASC)
  2. "," でフィールドと方向を分割
  3. ALLOWED_SORT_FIELDS.contains(field) == false → DEFAULT
  4. ALLOWED_DIRECTIONS.contains(direction) == false → direction = "asc"
  5. Sort.by(direction, field) を返す
       |
       v
[ResourceRepository] / [Comparator]
  検証済みの Sort のみが DB/Java ソートロジックに渡される
```

**設計根拠**:
- Controller を薄く保ち（パラメータ受け取りのみ）、ビジネスロジック（バリデーション）は Service に集約
- Spring Data `Sort` オブジェクトに渡る前にアプリレベルでホワイトリスト検証
- インジェクション攻撃（field に任意の SQL カラム名を注入しようとする試み）をブロック

**実装クラス**: `ResourceService.java`（パッケージ: `application`）

---

## 2. フォールバックパターン（SECURITY-09 / SECURITY-15）

### パターン: サイレントフォールバック（Silent Fallback Pattern）

```
無効な sort パラメータ検出
       |
       v
HTTP 4xx / 5xx を返さない
       |
       v
DEFAULT (createdAt, ASC) を使用
       |
       v
ログ出力: WARN レベルで記録
  "Invalid sort param: '{}', falling back to default. field={}, direction={}"
```

**設計根拠**:
- ユーザー体験優先（入力ミスで画面が壊れない）
- SECURITY-09: スタックトレースをクライアントに露出しない
- SECURITY-15: 例外を安全に処理（fail-safe デフォルト）
- ログ記録: 不審なアクセスパターンの検知に利用可能

---

## 3. ストラテジーパターン（2分岐ソートロジック）

### パターン: ルート分岐による Sort 適用戦略（Route-Based Sort Strategy）

```
from/to パラメータ有無で分岐
       |
       +-- from/to なし --> listPaginated
       |                   SortParam → Spring Data Sort → Pageable
       |                   DB が ORDER BY を実行（インデックス活用）
       |
       +-- from/to あり --> listWithAvailabilityFilter
                           SortParam → Comparator<Resource>
                           Java Stream.sorted() で適用
                           （DB から全件取得後、フィルタ → ソート → ページ切り出し）
```

**Comparator 生成ロジック**:

```
buildComparator(SortParam param) → Comparator<Resource>
  "name":      Comparator.comparing(Resource::getName, CASE_INSENSITIVE_ORDER)
  "capacity":  Comparator.comparingInt(Resource::getCapacity)
  "createdAt": Comparator.comparing(Resource::getCreatedAt)
  ↓
  param.direction == DESC → comparator.reversed()
```

---

## 4. PBT テスト設計パターン（PBT-03/04/07/10）

### パターン: 例題テスト + プロパティテスト補完（Complementary Testing Pattern）

```
ResourceServiceTest.java          (Example-Based)
  testListPaginatedSortByName()   ← 具体的なシナリオを固定
  testListPaginatedSortByCapacity()
  testDefaultSortApplied()
  testInvalidSortFallsBackToDefault()

ResourceSortPropertyTest.java     (Property-Based, 分離管理)
  @Property sortPreservesCount()  ← P-01: 件数不変
  @Property sortSatisfiesOrdering() ← P-02: 順序関係
  @Property sortIsIdempotent()    ← P-03: Idempotence
```

**jqwik テスト構造**:

```java
@ExtendWith(SpringExtension.class)
class ResourceSortPropertyTest {

    @Property
    void sortPreservesCount(@ForAll("resources") List<Resource> resources,
                            @ForAll("sortParams") String sortParam) {
        List<Resource> sorted = service.sortResources(resources, sortParam);
        assertThat(sorted).hasSize(resources.size());  // P-01
    }

    @Property
    void sortSatisfiesOrdering(@ForAll("resources") List<Resource> resources,
                               @ForAll("sortParams") String sortParam) {
        List<Resource> sorted = service.sortResources(resources, sortParam);
        // 隣接要素の順序関係を検証 (P-02)
    }

    @Property
    void sortIsIdempotent(@ForAll("resources") List<Resource> resources,
                          @ForAll("sortParams") String sortParam) {
        List<Resource> once = service.sortResources(resources, sortParam);
        List<Resource> twice = service.sortResources(once, sortParam);
        assertThat(twice).isEqualTo(once);  // P-03
    }

    @Provide
    Arbitrary<List<Resource>> resources() { /* ドメインジェネレータ */ }

    @Provide
    Arbitrary<String> sortParams() {
        return Arbitraries.of("name,asc", "name,desc", "capacity,asc",
                              "capacity,desc", "createdAt,asc", "createdAt,desc");
    }
}
```

---

## 5. その他パターン

| パターン | 適用 | 内容 |
|---|---|---|
| Resilience Patterns | N/A | sort は optional パラメータ。フォールバックが resilience を担保 |
| Scalability Patterns | N/A | インフラ変更なし。DB インデックス（既存）で対応 |
| Performance Patterns | N/A | listPaginated は DB ORDER BY（インデックス活用）。パフォーマンス最適化は不要 |
| Caching Patterns | N/A | ソート選択はリクエスト単位で可変。キャッシュは既存設計で対応 |
