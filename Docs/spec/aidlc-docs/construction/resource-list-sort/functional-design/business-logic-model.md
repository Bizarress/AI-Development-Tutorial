---
type: functional-design
title: ビジネスロジックモデル — リソース一覧のソート順選択
unit: resource-list-sort
timestamp: 2026-06-24T10:25:00Z
---

# ビジネスロジックモデル — resource-list-sort

## 1. ソートパラメータ解析ロジック

### 入力

```
sort: string | undefined
  例: "name,asc" / "capacity,desc" / "createdAt,asc" / undefined
```

### 処理フロー

```
sortParam が undefined または空
    └─> DEFAULT: { field: "createdAt", direction: ASC }

sortParam を "," で分割 → [field, direction]
    └─> field が ALLOWED_FIELDS に含まれない
            └─> DEFAULT にフォールバック
    └─> direction が {"asc", "desc"} に含まれない
            └─> direction を "asc" に修正（field は有効のまま）
    └─> 両方有効
            └─> { field, direction } を返す
```

### 許可フィールド（ALLOWED_FIELDS）

| フィールド名 | 対応 DB カラム | 型 |
|---|---|---|
| `name` | `name` | String |
| `capacity` | `capacity` | Integer |
| `createdAt` | `created_at` | LocalDateTime |

### デフォルト

`{ field: "createdAt", direction: ASC }` — 既存動作を維持

---

## 2. バックエンド sort 適用ロジック（2分岐）

### 分岐条件

```
from/to パラメータ なし → listPaginated（DBソート）
from/to パラメータ あり → listWithAvailabilityFilter（Javaソート）
```

### 分岐A: listPaginated（DB ソート）

```
SortParam → Spring Data Sort オブジェクト生成
    Sort sort = Sort.by(Sort.Direction.fromString(direction), field)

Pageable に Sort を含める
    Pageable pageable = PageRequest.of(page, size, sort)

Repository に渡す（DB が ORDER BY を実行）
    resourceRepository.findPagedResources(keyword, category, pageable)
```

### 分岐B: listWithAvailabilityFilter（Java ソート）

```
fetchAllCandidates() → Stream<Resource>（全件）
↓
予約重複フィルタ適用（既存ロジック）
↓
SortParam から Comparator<Resource> を生成
    name:     Comparator.comparing(Resource::getName, String.CASE_INSENSITIVE_ORDER)
    capacity: Comparator.comparingInt(Resource::getCapacity)
    createdAt: Comparator.comparing(Resource::getCreatedAt)
    ↓（direction が DESC の場合 reversed()）
↓
stream.sorted(comparator).collect(toList())
↓
Pagination: subList(page * size, min((page+1)*size, total))
```

---

## 3. フロントエンド sort パラメータ伝達フロー

```
shadcn/ui <Select>
    選択値: "name,asc" | "name,desc" | "capacity,asc" |
            "capacity,desc" | "createdAt,asc" | "createdAt,desc" | ""

onValueChange ハンドラ
    └─> value が "" → searchParams から sort を除去
    └─> value が非空 → searchParams.set("sort", value)
    └─> router.push(新 URL)

URL パラメータ → Server Action (resources.ts)
    searchParams.get("sort") → sort?: string

Server Action → バックエンド API リクエスト
    fetch(`/api/resources?...(既存)...&sort=${sort}`)
    （sort が undefined なら sort パラメータ不含）
```

---

## 4. PBT プロパティ（テスト設計基盤）

### P-01: Invariant — 件数不変

```
∀ SortParam sp, List<Resource> resources:
  sort(resources, sp).size() == resources.size()
```

意味: ソート操作は要素を追加・削除しない

### P-02: Invariant — 順序関係

```
∀ SortParam sp, List<Resource> sorted = sort(resources, sp):
  ∀ i < j: comparator(sp).compare(sorted[i], sorted[j]) <= 0
```

意味: ソート結果の隣接要素は指定フィールド・方向の順序を満たす

### P-03: Idempotence

```
∀ SortParam sp, List<Resource> resources:
  sort(sort(resources, sp), sp) == sort(resources, sp)
```

意味: 同一の sort パラメータを2回適用しても1回と同じ結果
