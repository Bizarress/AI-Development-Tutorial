---
type: unit-test-instructions
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# ユニットテスト実行手順 — keyword-search

## バックエンド — 全テスト実行

```bash
cd /workspace/backend

# Spotless フォーマット適用 → 全テスト実行
./gradlew spotlessApply test
```

### テスト対象クラスと期待件数

| テストクラス | 種別 | 追加テスト | 期待 |
|---|---|---|---|
| `ResourceServiceTest` | JUnit 5 + Mockito | keyword 例ベース 6 件 + stub 更新 | 全件 pass |
| `ResourceKeywordFilterPropertyTest` | jqwik PBT | P-01〜P-05（各 100 試行） | 全件 pass |
| `ResourceControllerTest` | Spring Boot Test（H2） | keyword 統合テスト 2 件 | 全件 pass |

### 特定テストクラスのみ実行

```bash
# ResourceServiceTest のみ
./gradlew test --tests "com.example.bookflow.application.ResourceServiceTest"

# PBT のみ
./gradlew test --tests "com.example.bookflow.application.ResourceKeywordFilterPropertyTest"

# ResourceControllerTest のみ
./gradlew test --tests "com.example.bookflow.presentation.ResourceControllerTest"
```

### テストレポート

```
backend/build/reports/tests/test/index.html
```

---

## 既存テストの確認ポイント

`ResourceServiceTest.List_` 内の既存 4 テストは stub が変わっています:

| 変更前 | 変更後 |
|---|---|
| `findByIsActiveTrue(pageable)` | `findWithFilters(false, null, null, pageable)` |
| `findAll(pageable)` | `findWithFilters(true, null, null, pageable)` |
| `findByIsActiveTrue()` | `findAllCandidates(false, null)` |
| `resourceService.list(..., pageable)` | `resourceService.list(..., null, pageable)` |

assertion 内容（返却件数・ID）は変わらないため、同じ結果を期待できます。

---

## PBT 実行の注意事項

- jqwik はデフォルト 100 試行 × 各 Property を実行します
- 失敗時は seed 値が出力されます（再現: `@Property(seed = <seed>)` を追加）
- CI では毎回異なる seed で実行されます（失敗時にログから seed を確認）

---

## フロントエンド — 型チェック

```bash
cd /workspace/frontend
npx tsc --noEmit
```

TypeScript エラーがないことを確認します。特に:
- `ResourceFilterForm.tsx`: `defaultKeyword` props の型
- `page.tsx`: `SearchParams.keyword` の型
- `resources.ts`: `ListResourcesParams.keyword` の型
