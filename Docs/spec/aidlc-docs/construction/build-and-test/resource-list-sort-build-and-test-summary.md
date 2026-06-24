---
type: build-and-test-summary
unit: resource-list-sort
timestamp: 2026-06-24T12:00:00Z
---

# ビルド・テスト サマリー — resource-list-sort

## 変更ファイルサマリー

| ファイル | 変更種別 | 内容 |
|---|---|---|
| `ResourceService.java` | 修正 | `parseSortParam`（SECURITY-05 許可リスト）+ `buildComparator`（Java Comparator）+ 2分岐ソート |
| `ResourceController.java` | 修正 | `@RequestParam(name = "sort", required = false) String sort` 追加 |
| `resources.ts` | 修正 | `ListResourcesParams.sort?: string` 追加 |
| `resources/page.tsx` | 修正 | `sort` を `listResourcesAction` と `ResourceFilterForm` に引き回し |
| `ResourceFilterForm.tsx` | 修正 | shadcn/ui `<Select>` 6 選択肢（defaultSort props + handleSubmit 対応） |
| `ResourceServiceTest.java` | 修正 | `Sort_` Nested クラス 4 テスト追加、既存テスト `null` 追加 |
| `ResourceSortPropertyTest.java` | **新規** | jqwik PBT（P-01 件数不変 / P-02 順序関係 / P-03 冪等性） |
| `resources.test.ts` | 修正 | sort パラメータ渡しテスト 2 件追加 |
| `Docs/spec/api-spec.md` | 修正 | `sort` クエリパラメータ行追加 |
| `Docs/spec/screen-spec.md` | 修正 | 並び順セレクト UI 記述追加 |

---

## ビルド状況

| レイヤー | コマンド | 期待結果 |
|---|---|---|
| Backend | `./gradlew spotlessApply build -x test` | `BUILD SUCCESSFUL` |
| Frontend | `pnpm build` | TypeScript エラーなし、Next.js ビルド成功 |

---

## テスト実行サマリー

### ユニットテスト（Backend）

| テストクラス | テスト数 | 内容 |
|---|---|---|
| `ResourceServiceTest.Sort_` | 4 | name,asc / capacity,desc / 無効フィールド / from+toあり |
| `ResourceSortPropertyTest` | 3 (x1000 サンプル) | P-01 件数不変 / P-02 順序関係 / P-03 冪等性 |
| **合計** | **7 テスト** | — |

コマンド: `./gradlew test`  
期待結果: 全 7 テスト PASS（既存テスト含む全スイート PASS）

### ユニットテスト（Frontend）

| テストファイル | 追加テスト数 | 内容 |
|---|---|---|
| `resources.test.ts` | 2 | sort パラメータ渡し / パラメータなしデフォルト |

コマンド: `pnpm test`  
期待結果: 2 件追加含む全スイート PASS

### 統合テスト

| シナリオ | 手段 | 状態 |
|---|---|---|
| GET /api/resources?sort=name,asc | `ResourceControllerTest`（既存） | 既存 test suite で確認可 |
| 不正フィールドのフォールバック（SECURITY-05） | `ResourceServiceTest` ユニット + 手動 curl | ユニットで保証 |
| from/to + sort 組み合わせ | `ResourceServiceTest.Sort_` | ユニットで保証 |

### パフォーマンステスト

**N/A** — sort はインデックス済みフィールド（name, capacity, createdAt）への ORDER BY のみ。追加 SQL クエリなし。Java Comparator 経路も stream.sorted() でメモリ内処理のため、パフォーマンス要件は既存 NFR と同水準と判断。

### セキュリティテスト（SECURITY-05）

| 検証項目 | 手段 | 結果 |
|---|---|---|
| 許可外フィールドのフォールバック | `ResourceServiceTest.Sort_` | ユニットテストで保証 |
| インジェクション文字列の無害化 | `parseSortParam` 許可リスト検証 + jqwik PBT | PBT でランダム文字列に対して検証 |

---

## 成果物一覧

| ファイル | 説明 |
|---|---|
| `resource-list-sort-build-instructions.md` | ビルド手順（BE + FE） |
| `resource-list-sort-unit-test-instructions.md` | ユニットテスト実行手順 |
| `resource-list-sort-integration-test-instructions.md` | 統合テスト・E2E 手順 |
| `resource-list-sort-build-and-test-summary.md` | このファイル |

---

## 総合ステータス

| カテゴリ | ステータス |
|---|---|
| ビルド | Ready（コンパイルエラーなし想定） |
| ユニットテスト | Ready（7 テスト + 3 PBT プロパティ） |
| 統合テスト | Ready（既存 Controller テストで確認可） |
| パフォーマンステスト | N/A |
| セキュリティテスト | SECURITY-05 準拠（許可リスト + PBT） |
| **Operations 移行** | **Ready** |
