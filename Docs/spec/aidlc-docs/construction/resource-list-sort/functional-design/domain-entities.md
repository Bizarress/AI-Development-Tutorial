---
type: functional-design
title: ドメインエンティティ — リソース一覧のソート順選択
unit: resource-list-sort
timestamp: 2026-06-24T10:25:00Z
---

# ドメインエンティティ — resource-list-sort

> 本エンハンスは既存エンティティの変更を伴わない。ここでは sort 機能に関連する値オブジェクト・データ構造のみを定義する。

## 既存エンティティ（参照のみ）

### Resource

既存の `Resource` エンティティ。ソート対象フィールドを確認する。

| フィールド | 型 | ソート対象 |
|---|---|---|
| `name` | String | Yes — BR-07 大文字小文字区別なし |
| `capacity` | Integer | Yes |
| `createdAt` | LocalDateTime | Yes — デフォルトソートキー |
| その他フィールド | 各種 | No |

---

## 新規値オブジェクト

### SortParam（バックエンド）

sort パラメータの解析結果を表す値オブジェクト。

```
SortParam {
  field: SortField      // ALLOWED_FIELDS のいずれか
  direction: SortDirection // ASC | DESC
}
```

| 属性 | 型 | 制約 |
|---|---|---|
| `field` | SortField | ALLOWED_FIELDS のみ |
| `direction` | SortDirection | ASC または DESC |

```
SortField = "name" | "capacity" | "createdAt"
SortDirection = ASC | DESC
```

**静的ファクトリ**:
```
SortParam.parse(String sortParam) → SortParam
  - null / 空 → DEFAULT (createdAt, ASC)
  - 無効フィールド → DEFAULT
  - 無効方向 → field はそのまま、direction を ASC に修正
```

**デフォルト定数**:
```
SortParam.DEFAULT = SortParam { field: "createdAt", direction: ASC }
```

### ALLOWED_FIELDS（定数セット）

```
Set<String> ALLOWED_FIELDS = {"name", "capacity", "createdAt"}
```

---

## データ転送

### フロントエンド → バックエンド（URL パラメータ）

```
sort: string | undefined
  形式: "field,direction"
  例: "name,asc", "capacity,desc", "createdAt,asc"
  未指定: sort パラメータ省略
```

### バックエンド内部

```
SortParam → Spring Data Sort (listPaginated ルート)
SortParam → Comparator<Resource> (listWithAvailabilityFilter ルート)
```

---

## コンポーネント関連（既存、変更なし）

```
ResourceController  ─uses─> ResourceService
ResourceService     ─uses─> ResourceRepository
ResourceRepository  ─extends─> JpaRepository<Resource, UUID>
```

変更は上記3クラスのメソッドシグネチャ拡張のみ。エンティティ間のリレーションシップ変更なし。
