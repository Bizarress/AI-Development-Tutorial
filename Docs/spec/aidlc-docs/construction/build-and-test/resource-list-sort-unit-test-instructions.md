---
type: unit-test-instructions
unit: resource-list-sort
timestamp: 2026-06-24T12:00:00Z
---

# ユニットテスト実行手順 — resource-list-sort

## バックエンド（JUnit 5 + jqwik）

### 全テスト実行

```bash
cd /workspace/backend
./gradlew test
```

### ターゲットを絞って実行

```bash
# sort 関連のみ実行（高速確認用）
./gradlew test --tests "com.example.bookflow.application.ResourceServiceTest\$List_\$Sort_*"
./gradlew test --tests "com.example.bookflow.application.ResourceSortPropertyTest"
```

### 期待するテスト結果

#### ResourceServiceTest — Sort_ Nested クラス

| テストメソッド | 検証内容 |
|---|---|
| `list_sortByNameAsc_returnsNameAscendingOrder` | name 昇順ソートが正しく適用される |
| `list_sortByCapacityDesc_returnsCapacityDescendingOrder` | capacity 降順ソートが正しく適用される |
| `list_invalidSortField_fallsBackToDefault` | 許可外フィールド → デフォルト（createdAt,asc）フォールバック |
| `list_sortWithTimeFilter_appliesComparatorSort` | from/to フィルタあり → Java Comparator ソートが適用される |

**期待**: 4 テスト全 PASS

#### ResourceSortPropertyTest — jqwik PBT

| プロパティ | 検証内容 | 試行数 |
|---|---|---|
| `sortPreservesCount` (P-01) | sort(list, p).size() == list.size() | 1000 回 |
| `sortSatisfiesOrdering` (P-02) | 隣接要素がソート条件を満たす | 1000 回 |
| `sortIsIdempotent` (P-03) | sort(sort(list,p),p) == sort(list,p) | 1000 回 |

**期待**: 3 プロパティ全 PASS（3000 サンプル以上でランダム検証）

### テストレポート確認

```bash
# HTML レポート（ブラウザで確認）
open backend/build/reports/tests/test/index.html

# jqwik レポート（失敗時のシュリンク確認）
cat backend/build/reports/tests/test/classes/com.example.bookflow.application.ResourceSortPropertyTest.html
```

---

## フロントエンド（Vitest）

### 全テスト実行

```bash
cd /workspace/frontend
pnpm test
```

### ターゲットを絞って実行

```bash
# resources.test.ts のみ
pnpm test tests/unit/server/actions/resources.test.ts
```

### 期待するテスト結果

#### resources.test.ts — listResourcesAction

| テストケース | 検証内容 |
|---|---|
| `sort パラメータを渡せる` | `listResourcesAction({ sort: "name,asc" })` が 1 件返す |
| `sort パラメータなしで呼び出せる（デフォルト動作）` | `listResourcesAction({})` が正常終了する |

**期待**: 2 件追加含む全テスト PASS

### カバレッジ確認（任意）

```bash
pnpm test -- --coverage
```

---

## よくある失敗と対処

### `capacity` comparator で NullPointerException

`buildComparator` の capacity ケースが `Comparator.nullsLast` を使っているか確認：

```java
case "capacity" ->
    Comparator.comparing(
        Resource::getCapacity,
        Comparator.nullsLast(Comparator.naturalOrder()));
```

### jqwik が `@Property` を認識しない

`ResourceSortPropertyTest` クラスが JUnit Platform に登録されるには `build.gradle.kts` に jqwik が `testImplementation` で含まれていること。keyword-search エンハンスで追加済みのため通常は不要。確認：

```bash
./gradlew dependencies --configuration testRuntimeClasspath | grep jqwik
```
