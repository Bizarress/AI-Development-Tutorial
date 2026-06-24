---
type: functional-design
artifact: frontend-components
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# フロントエンドコンポーネント設計 — keyword-search

## 変更対象コンポーネント

### ResourceFilterForm（`ResourceFilterForm.tsx`）

#### Props 変更

```typescript
// 変更前
interface ResourceFilterFormProps {
  defaultCategory?: string;
  defaultFrom?: string;
  defaultTo?: string;
}

// 変更後（defaultKeyword 追加）
interface ResourceFilterFormProps {
  defaultCategory?: string;
  defaultFrom?: string;
  defaultTo?: string;
  defaultKeyword?: string;   // 追加
}
```

#### State・フォームデータフロー

```
ユーザー入力 (keyword input)
        |
        v
FormData.get("keyword")
        |
        v
handleSubmit():
  - keyword が非空文字のとき: params.set("keyword", keyword)
  - keyword が空文字のとき: params に keyword を設定しない（フィルタ解除）
        |
        v
router.push(`/resources?${params.toString()}`)
        |
        v
サーバーコンポーネント（page.tsx）が searchParams.keyword を読み取り
        |
        v
ResourceFilterForm に defaultKeyword として渡す
API クライアントが GET /api/resources?keyword=... を呼び出す
```

#### レイアウト変更

現在 `grid-cols-1 sm:grid-cols-3`（カテゴリ・開始・終了の3列）を **`sm:grid-cols-4`（+キーワード列）** に変更する。

```
+----------+----------+----------+----------+
| カテゴリ  | キーワード | 開始日時  | 終了日時  |
+----------+----------+----------+----------+
```

> **代替案**: `sm:grid-cols-3` のままにして、キーワードを別行に配置する（デザイン判断）。実装時に視覚的に確認する。

#### 追加 JSX（キーワード入力フィールド）

```tsx
{/* キーワード */}
<div className="space-y-1">
  <Label htmlFor="keyword">キーワード</Label>
  <Input
    id="keyword"
    name="keyword"
    type="text"
    placeholder="名称・説明で検索"
    defaultValue={defaultKeyword ?? ""}
  />
</div>
```

#### handleSubmit 変更箇所

```typescript
const keyword = data.get("keyword") as string;
// ...
if (keyword && keyword.trim()) params.set("keyword", keyword.trim());
```

---

### 親コンポーネント（`page.tsx`）

`/workspace/frontend/src/app/(authenticated)/resources/page.tsx` にて `searchParams.keyword` を受け取り Props に渡す。

```typescript
// 変更前
<ResourceFilterForm
  defaultCategory={searchParams.category}
  defaultFrom={searchParams.from}
  defaultTo={searchParams.to}
/>

// 変更後
<ResourceFilterForm
  defaultCategory={searchParams.category}
  defaultFrom={searchParams.from}
  defaultTo={searchParams.to}
  defaultKeyword={searchParams.keyword}   // 追加
/>
```

また、API 呼び出し時に `keyword` を渡す箇所（`fetchResources` 等）も確認・更新が必要。

---

## ユーザーインタラクションフロー

```
1. ユーザーがキーワード入力欄に "会議室" と入力
2. 「絞り込む」ボタンをクリック
3. handleSubmit が keyword="会議室" を URL パラメータに追加
4. router.push("/resources?keyword=会議室") でページ遷移
5. サーバーコンポーネントが searchParams.keyword = "会議室" を読み取る
6. API GET /api/resources?keyword=会議室 を呼び出す
7. 結果がフィルタされて表示される
8. フォームの keyword フィールドに "会議室" が初期値として表示される（defaultKeyword）

リセット時:
- handleReset が router.push("/resources") を呼び出す
- keyword を含むすべての params がクリアされる（既存動作で対応済み）
```

---

## バリデーション（フロントエンド）

- 空文字・空白のみ → URL パラメータに含めない（= バックエンドが keyword=null として扱う）
- XSS 対策: React の JSX エスケープにより自動的に保護される
- 長さ制限: なし（Q1=B、バックエンドも制限なし）
