---
type: functional-design
title: フロントエンドコンポーネント — リソース一覧のソート順選択
unit: resource-list-sort
timestamp: 2026-06-24T10:25:00Z
---

# フロントエンドコンポーネント — resource-list-sort

## コンポーネント階層（変更箇所）

```
page.tsx (Server Component)
  └─ ResourceFilterForm.tsx (Client Component) ← 変更: Sort Select 追加
      └─ <Select> (shadcn/ui) ← 新規追加
```

---

## ResourceFilterForm.tsx の変更詳細

### 追加 Props

変更なし（`searchParams` を URL から受け取る既存パターンを流用）

### 追加 State

```typescript
const [sortValue, setSortValue] = useState<string>(
  searchParams.get("sort") ?? ""
)
```

### Sort Select コンポーネント

```typescript
<Select
  value={sortValue}
  onValueChange={(value) => {
    setSortValue(value)
    const params = new URLSearchParams(searchParams.toString())
    if (value === "") {
      params.delete("sort")
    } else {
      params.set("sort", value)
    }
    router.push(`/resources?${params.toString()}`)
  }}
>
  <SelectTrigger>
    <SelectValue placeholder="並び順" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="createdAt,asc">登録日時（古い順）</SelectItem>
    <SelectItem value="createdAt,desc">登録日時（新しい順）</SelectItem>
    <SelectItem value="name,asc">名称（昇順）</SelectItem>
    <SelectItem value="name,desc">名称（降順）</SelectItem>
    <SelectItem value="capacity,asc">定員（少ない順）</SelectItem>
    <SelectItem value="capacity,desc">定員（多い順）</SelectItem>
  </SelectContent>
</Select>
```

### Select 選択肢マッピング

| 表示ラベル | sort パラメータ値 |
|---|---|
| 登録日時（古い順） | `createdAt,asc` |
| 登録日時（新しい順） | `createdAt,desc` |
| 名称（昇順） | `name,asc` |
| 名称（降順） | `name,desc` |
| 定員（少ない順） | `capacity,asc` |
| 定員（多い順） | `capacity,desc` |

---

## resources.ts（Server Action）の変更詳細

### 変更箇所

```typescript
// 現在の signature（変更前）
export async function getResources(params: ResourceSearchParams) {
  // ...
}

// 変更後: sort を追加
interface ResourceSearchParams {
  page?: number
  keyword?: string
  category?: string
  from?: string
  to?: string
  sort?: string   // 追加
}
```

### API リクエスト組み立て

```typescript
const query = new URLSearchParams()
if (params.keyword) query.set("keyword", params.keyword)
if (params.category) query.set("category", params.category)
if (params.from) query.set("from", params.from)
if (params.to) query.set("to", params.to)
if (params.sort) query.set("sort", params.sort)   // 追加
// page, size ...
```

---

## ユーザーインタラクションフロー

```
1. ユーザーが Sort Select を開く
2. 選択肢（6種）から1つ選択
3. onValueChange が発火
4. URL パラメータを更新（sort=field,direction）
5. router.push でページ遷移（SSR で再フェッチ）
6. バックエンドが sort パラメータを受け取りソート結果を返す
7. 画面が更新される

リセット時:
  router.push("/resources") → sort パラメータ消去 → デフォルト順
```

---

## フォームバリデーション

フロントエンド側での sort パラメータバリデーションは不要。
- Select の選択肢が固定のため、不正な値がユーザー操作で入力されない
- バックエンドが SECURITY-05 準拠の許可リスト検証で最終防衛

---

## API 統合ポイント

| コンポーネント | 呼び出す API | パラメータ |
|---|---|---|
| `ResourceFilterForm.tsx` | URL 更新（SSR リロード） | `sort=field,direction` |
| `resources.ts` (Server Action) | `GET /api/resources` | `sort=field,direction` |
