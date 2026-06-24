---
type: code-generation-plan
title: Code Generation 計画 — リソース一覧のソート順選択
unit: resource-list-sort
status: Completed
timestamp: 2026-06-24T10:55:00Z
---

# Code Generation 計画 — resource-list-sort

## 対象ファイル一覧（Brownfield — 既存ファイルを修正）

| # | ファイルパス | 変更種別 |
|---|---|---|
| 1 | `backend/src/main/java/com/example/bookflow/application/ResourceService.java` | 修正 |
| 2 | `backend/src/main/java/com/example/bookflow/presentation/ResourceController.java` | 修正 |
| 3 | `frontend/src/server/actions/resources.ts` | 修正 |
| 4 | `frontend/src/app/(authenticated)/resources/page.tsx` | 修正 |
| 5 | `frontend/src/app/(authenticated)/resources/ResourceFilterForm.tsx` | 修正 |
| 6 | `backend/src/test/java/com/example/bookflow/application/ResourceServiceTest.java` | 修正 |
| 7 | `backend/src/test/java/com/example/bookflow/application/ResourceSortPropertyTest.java` | 新規 |
| 8 | `frontend/tests/unit/server/actions/resources.test.ts` | 修正 |
| 9 | `Docs/spec/api-spec.md` | 修正 |
| 10 | `Docs/spec/screen-spec.md` | 修正 |

---

## 実行ステップ

### Step 1: ResourceService.java — sort ロジック追加

- [x] `ALLOWED_SORT_FIELDS = Set.of("name", "capacity", "createdAt")` 定数を追加
- [x] `parseSortParam(String sortParam) → Sort` を追加（SECURITY-05 許可リスト検証）
  - null/空 → `Sort.by(ASC, "createdAt")`（デフォルト）
  - フィールドが ALLOWED_SORT_FIELDS 外 → デフォルト
  - 方向が "asc"/"desc" 外 → "asc" に修正、フィールドはそのまま
- [x] `buildComparator(Sort sort) → Comparator<Resource>` を追加（listWithAvailabilityFilter 用）
  - "name": `Comparator.comparing(Resource::getName, String.CASE_INSENSITIVE_ORDER)`
  - "capacity": `Comparator.comparing(Resource::getCapacity, nullsLast(naturalOrder()))`（nullable 対応）
  - "createdAt": `Comparator.comparing(Resource::getCreatedAt)`
  - DESC の場合: `.reversed()`
- [x] `list()` シグネチャに `String sortParam` を追加
  - `parseSortParam(sortParam)` を呼び出し `Sort sort` を生成
  - `listPaginated()` に `sort` を渡す
  - `listWithAvailabilityFilter()` に `sort` を渡す
- [x] `listPaginated()` シグネチャに `Sort sort` を追加
  - `Pageable` を `PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)` に再生成
- [x] `listWithAvailabilityFilter()` シグネチャに `Sort sort` を追加
  - フィルタ後の `candidates` に `Comparator<Resource>` を適用
  - ソート後にページネーション（既存の subList ロジックを維持）
- [x] 必要な import 追加（`Sort`, `PageRequest`, `Comparator`, `Set`）

### Step 2: ResourceController.java — sort パラメータ追加

- [x] `list()` メソッドに `@RequestParam(name = "sort", required = false) String sort` を追加
- [x] `resourceService.list()` 呼び出しに `sort` を追加
- [x] Javadoc コメントに `@param sort ソートパラメータ（フィールド,方向 形式）` を追記

### Step 3: resources.ts — sort パラメータ追加

- [x] `ListResourcesParams` に `sort?: string` を追加
- [x] `listResourcesAction` の `queryParams` 構築部分に `if (params?.sort) queryParams.sort = params.sort` を追加

### Step 4: page.tsx — sort パラメータ追加

- [x] `SearchParams` インターフェースに `sort?: string` を追加
- [x] `listResourcesAction()` 呼び出しに `sort: params.sort` を追加
- [x] `ResourceFilterForm` に `defaultSort={params.sort}` Props を追加

### Step 5: ResourceFilterForm.tsx — ソート選択 UI 追加

- [x] `ResourceFilterFormProps` に `defaultSort?: string` を追加
- [x] コンポーネント関数の引数分解に `defaultSort` を追加
- [x] `handleSubmit` 内の `params` 構築に sort 処理を追加
  - `const sort = data.get("sort") as string` で値を取得
  - `sort && sort !== ""` の場合 `params.set("sort", sort)`
- [x] フォーム内にソート `<Select>` を追加（`name="sort"`）
  - `defaultValue={defaultSort ?? ""}` で初期値を設定
  - `data-testid="sort-select"` を `SelectTrigger` に付与
  - 選択肢 6件:
    - `value=""`: 「並び順（デフォルト）」
    - `value="createdAt,asc"`: 「登録日時（古い順）」
    - `value="createdAt,desc"`: 「登録日時（新しい順）」
    - `value="name,asc"`: 「名称（昇順）」
    - `value="name,desc"`: 「名称（降順）」
    - `value="capacity,asc"`: 「定員（少ない順）」
    - `value="capacity,desc"`: 「定員（多い順）」

### Step 6: ResourceServiceTest.java — ソートケース追加

- [x] `makeResource` ヘルパーに `capacity` を設定できるオーバーロードを追加
- [x] `List_` Nested クラス内に `Sort_` Nested クラスを新規追加
  - `list_sortByNameAsc_returnsNameAscendingOrder()`: name 昇順で返ること
  - `list_sortByCapacityDesc_returnsCapacityDescendingOrder()`: capacity 降順で返ること
  - `list_invalidSortField_fallsBackToDefault()`: 無効フィールド → デフォルト（呼び出し成功）
  - `list_sortWithTimeFilter_appliesComparatorSort()`: from/to ありで Java ソートが適用される

### Step 7: ResourceSortPropertyTest.java — 新規 PBT クラス

- [x] パッケージ: `com.example.bookflow.application`
- [x] `makeResource(String name, int capacity, LocalDateTime createdAt)` ヘルパー
- [x] `applySort(List<Resource> resources, String sortParam)` ヘルパー（ResourceService のソートロジックを複製）
- [x] ジェネレータ (`@Provide`):
  - `resourceLists()`: name(英字1〜30)・capacity(1〜500)・createdAt(エポック範囲) の Resource リスト
  - `sortParams()`: `Arbitraries.of("name,asc", "name,desc", "capacity,asc", "capacity,desc", "createdAt,asc", "createdAt,desc")`
- [x] プロパティテスト:
  - `@Property sortPreservesCount()`: `sort(list, p).size() == list.size()` (P-01)
  - `@Property sortSatisfiesOrdering()`: 隣接要素の順序関係（P-02）
  - `@Property sortIsIdempotent()`: `sort(sort(list, p), p) == sort(list, p)` (P-03)

### Step 8: resources.test.ts — sort 引き回しテスト追加

- [x] `listResourcesAction` の describe ブロックに sort テストケースを追加:
  - `"sort パラメータを渡せる"`: `listResourcesAction({ sort: "name,asc" })` が成功すること

### Step 9: api-spec.md — sort パラメータ追記

- [x] `GET /api/resources` のクエリパラメータ表に `sort` 行を追加
- [x] 説明文に sort の動作（許可フィールド・デフォルト・フォールバック）を追記
- [x] サンプルリクエスト URL に `sort=name,asc` を含む例を追加

### Step 10: screen-spec.md — ソート選択 UI 追記

- [x] `/resources` のフィルタフォームセクションにソート選択 UI の記述を追加
- [x] 選択肢リスト・デフォルト動作・リセット時の挙動を記述

---

## 依存関係

```
Step 1 (ResourceService) → Step 2 (ResourceController) 依存
Step 3 (resources.ts) → Step 4 (page.tsx) → Step 5 (ResourceFilterForm) 依存
Step 1 → Step 6 (ResourceServiceTest) 依存
Step 1 → Step 7 (ResourceSortPropertyTest) 依存
Step 3 → Step 8 (resources.test.ts) 依存
Step 1, 2 → Step 9 (api-spec.md) 依存
Step 5 → Step 10 (screen-spec.md) 依存
```

## 推定ステップ数

10 ステップ（修正 9 ファイル + 新規 1 ファイル）
