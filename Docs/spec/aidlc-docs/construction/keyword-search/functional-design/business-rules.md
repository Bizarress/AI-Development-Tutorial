---
type: functional-design
artifact: business-rules
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# ビジネスルール — keyword-search

## BR-01: keyword の有効条件

| 入力値 | 扱い |
|---|---|
| `null`（パラメータ未指定） | フィルタなし（全件対象） |
| `""`（空文字） | フィルタなし（全件対象） |
| `" "` 等（空白のみ） | フィルタなし（`isBlank()` で null 化） |
| `"会議室"` 等（1文字以上の非空白） | keyword フィルタを適用 |

**理由**: Q1=B でバリデーションなしが決定。"フィルタなし" の意味として null/blank を統一扱いする。

---

## BR-02: マッチング条件

- **対象フィールド**: `resources.name`（NOT NULL）および `resources.description`（nullable）
- **条件**: `name` または `description` のいずれかに keyword が部分一致すれば結果に含まれる（OR 条件）
- **大文字小文字**: 区別しない（LOWER + LIKE、または `String.toLowerCase().contains()`）
- **description が NULL の場合**: マッチしない（NULL は比較対象から除外）

---

## BR-03: 既存フィルタとの組み合わせ（AND 条件）

`category`・`from`/`to`・`keyword` はすべて AND 条件で組み合わされる。

```
最終結果 = isActive条件 AND category条件 AND from/to条件 AND keyword条件
```

各条件は未指定（null）の場合は適用されない（=全件対象）。

---

## BR-04: セキュリティルール（SECURITY-05 準拠）

- `keyword` は JPQL の名前付きパラメータ（`:keyword`）としてバインドする
- 文字列連結による JPQL 組み立ては禁止（SQL インジェクション防止）
- 例: `"... LIKE '%" + keyword + "%'"` → **禁止**
- 例: `LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))` → **許可**

Java Stream 側（`listWithAvailabilityFilter`）は JPA を介さないため SQL インジェクションリスクはないが、同様に `String.toLowerCase().contains(keyword.toLowerCase())` を使用する。

---

## BR-05: ページネーションとの整合

- `listPaginated` パス: DB 側でフィルタ後にページネーション → total は keyword 適用後の件数
- `listWithAvailabilityFilter` パス: Java 側でフィルタ後に `PageImpl` で手動ページネーション → total は keyword + 空き確認フィルタ後の件数
- 既存の `pageable`（sort・size）はそのまま適用される（変更なし）

---

## BR-06: 後方互換性

- `keyword` パラメータは `required = false`（デフォルト null）
- `keyword` 未指定時の動作は現行と完全一致
- 既存 `ResourceServiceTest`・`ResourceControllerTest` はすべて pass すること
