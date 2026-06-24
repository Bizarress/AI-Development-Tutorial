---
type: functional-design
title: ビジネスルール — リソース一覧のソート順選択
unit: resource-list-sort
timestamp: 2026-06-24T10:25:00Z
---

# ビジネスルール — resource-list-sort

## BR-01: sort フィールド許可リスト（SECURITY-05 準拠）

- `sort` パラメータのフィールド部分は `{name, capacity, createdAt}` のみ許可
- それ以外の文字列（例: `password`, `id`, `任意文字列`）はデフォルトにフォールバック
- フォールバック値: `createdAt,asc`
- **実装**: コントローラ or サービスで `ALLOWED_FIELDS` に対してホワイトリスト検証を行い、Spring Data `Sort` に渡す前に検証する

## BR-02: sort 方向の正規化

- 方向部分は `asc` または `desc` のみ許可（大文字小文字は正規化して受け入れる）
- それ以外 → `asc` にフォールバック
- フィールドが有効で方向のみ無効な場合は、方向を `asc` に修正して処理を続ける

## BR-03: sort 未指定時のデフォルト

- `sort` クエリパラメータが存在しない場合、デフォルト `createdAt,asc` を適用
- これは既存の動作（登録日時昇順固定）を維持するものであり、後方互換を保証する

## BR-04: sort とフィルタの組み合わせ

- `keyword`, `category`, `from`/`to` フィルタと `sort` は独立して組み合わせられる
- `from`/`to` あり（`listWithAvailabilityFilter`）の場合でも sort は適用される
  - ただし適用タイミングが異なる（フィルタ後の Java ソート）

## BR-05: エラーにしない（ユーザー体験優先）

- 無効な `sort` パラメータに対して HTTP エラー（400 等）を返さない
- サイレントにデフォルトへフォールバックする
- スタックトレース・エラーメッセージをクライアントに返さない（SECURITY-08 準拠）

## BR-06: ページネーションとの組み合わせ

- `listPaginated` では sort が `Pageable` に含まれるため、ページをまたいでも一貫したソート順が保証される
- `listWithAvailabilityFilter` では Java ソート後に手動でページネーションを適用するため、ソート→ページ切り出しの順番を厳守する

## BR-07: 大文字小文字の扱い（name フィールド）

- `name` フィールドでソートする場合、大文字小文字を区別しない（`CASE_INSENSITIVE_ORDER`）
- DB ソート（listPaginated）では `LOWER(name)` を使うか、Collation 設定に依存する
- Java ソート（listWithAvailabilityFilter）では `String.CASE_INSENSITIVE_ORDER` を使用

## BR-08: フロントエンドの sort 選択リセット

- フィルタリセット（`router.push("/resources")`）時に `sort` もクリアされる
- これは既存のリセット動作に sort が自然に含まれることを意味し、追加実装は不要
