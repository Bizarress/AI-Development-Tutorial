---
type: plan
unit: keyword-search
stage: Code Generation
status: Complete
timestamp: 2026-06-23T00:00:00Z
---

# Code Generation 計画 — keyword-search

## ユニット概要

- **ユニット**: keyword-search（単一ユニット）
- **プロジェクト種別**: Brownfield
- **ワークスペースルート**: /workspace
- **変更方式**: 既存ファイルをすべてインプレース修正（新規ファイルは作成しない）

---

## 変更ファイル一覧

| # | ファイル | 変更種別 |
|---|---|---|
| 1 | `backend/build.gradle.kts` | 修正（jqwik 追加）|
| 2 | `backend/src/main/java/.../domain/ResourceRepository.java` | 修正（@Query 追加）|
| 3 | `backend/src/main/java/.../application/ResourceService.java` | 修正（keyword 追加・リファクタ）|
| 4 | `backend/src/main/java/.../presentation/ResourceController.java` | 修正（@RequestParam 追加）|
| 5 | `backend/src/test/java/.../application/ResourceServiceTest.java` | 修正（stub 更新 + 新規テスト）|
| 6 | `backend/src/test/java/.../presentation/ResourceControllerTest.java` | 修正（keyword テスト追加）|
| 7 | `frontend/src/server/actions/resources.ts` | 修正（keyword パラメータ追加）|
| 8 | `frontend/src/app/(authenticated)/resources/ResourceFilterForm.tsx` | 修正（keyword 入力欄追加）|
| 9 | `frontend/src/app/(authenticated)/resources/page.tsx` | 修正（searchParams.keyword 追加）|
| 10 | `Docs/spec/api-spec.md` | 修正（keyword パラメータ追記）|
| 11 | `Docs/spec/screen-spec.md` | 修正（UI 仕様追記）|

---

## 実行ステップ

### Step 1: build.gradle.kts — jqwik 依存関係追加
- [x] `testImplementation("net.jqwik:jqwik:1.9.1")` を追加する
- **場所**: `backend/build.gradle.kts`
- **目的**: PBT-09（フレームワーク選択）、SECURITY-10（固定バージョン）

### Step 2: ResourceRepository — @Query メソッド追加
- [x] `findWithFilters(isAdmin, category, keyword, pageable)` を追加する（ページネーション用）
- [x] `findAllCandidates(isAdmin, category)` を追加する（全件取得用）
- **場所**: `backend/src/main/java/com/example/bookflow/domain/ResourceRepository.java`
- **目的**: DB レベル keyword フィルタ（listPaginated パス）＋ fetchAllCandidates 統合
- **制約**: `:keyword` は名前付きパラメータバインド（SECURITY-05）

### Step 3: ResourceService — keyword 追加・リファクタ
- [x] `list()` シグネチャに `String keyword` を追加する
- [x] `normalizeKeyword()` ヘルパーメソッドを追加する（null/blank → null）
- [x] `listPaginated()` を `findWithFilters()` を使うよう書き換える
- [x] `listWithAvailabilityFilter()` に Java Stream keyword フィルタを追加する
- [x] `fetchAllCandidates()` を `findAllCandidates()` を使うよう書き換える
- **場所**: `backend/src/main/java/com/example/bookflow/application/ResourceService.java`
- **目的**: 2パスへの keyword 適用（DB/Java それぞれ）
- **注意**: `Locale.ROOT` を使用した toLowerCase

### Step 4: ResourceController — keyword パラメータ追加
- [x] `list()` メソッドに `@RequestParam(required = false) String keyword` を追加する
- [x] `resourceService.list()` 呼び出しに `keyword` を追加する
- **場所**: `backend/src/main/java/com/example/bookflow/presentation/ResourceController.java`

### Step 5: ResourceServiceTest — stub 更新 + keyword テスト追加
- [x] 既存テストの stub を `findWithFilters` / `findAllCandidates` に更新する
- [x] 既存テストの `list(...)` 呼び出しに `null`（keyword）を追加する
- [x] keyword あり・なしの例ベーステストを追加する（`keyword_matchesName`, `keyword_matchesDescription`, `keyword_noMatch`, `keyword_caseInsensitive`, `keyword_null_returnsAll`, `keyword_blank_returnsAll`）
- [x] jqwik PBT を追加する（P-01〜P-05、`makeResource` に description フィールドを追加）
- **場所**: `backend/src/test/java/com/example/bookflow/application/ResourceServiceTest.java`

### Step 6: ResourceControllerTest — keyword テスト追加
- [x] `list_withKeyword_returnsFilteredResources` を追加する
- **場所**: `backend/src/test/java/com/example/bookflow/presentation/ResourceControllerTest.java`

### Step 7: resources.ts — keyword パラメータ追加
- [x] `ListResourcesParams` に `keyword?: string` を追加する
- [x] `listResourcesAction` 内で `keyword` を queryParams に追加する
- **場所**: `frontend/src/server/actions/resources.ts`

### Step 8: ResourceFilterForm.tsx — keyword 入力欄追加
- [x] `ResourceFilterFormProps` に `defaultKeyword?: string` を追加する
- [x] keyword `<Input>` フィールドを追加する（`data-testid="keyword-input"`）
- [x] `handleSubmit` に keyword の取得・URL パラメータ設定を追加する
- [x] グリッドレイアウトを `sm:grid-cols-3` → `sm:grid-cols-2 lg:grid-cols-4` に変更する
- **場所**: `frontend/src/app/(authenticated)/resources/ResourceFilterForm.tsx`

### Step 9: page.tsx — searchParams.keyword 追加
- [x] `SearchParams` 型に `keyword?: string` を追加する
- [x] `listResourcesAction` に `keyword: params.keyword` を追加する
- [x] `<ResourceFilterForm>` に `defaultKeyword={params.keyword}` を追加する
- **場所**: `frontend/src/app/(authenticated)/resources/page.tsx`

### Step 10: api-spec.md — keyword パラメータ追記
- [x] `GET /api/resources` のクエリパラメータ表に `keyword` を追加する
- **場所**: `Docs/spec/api-spec.md`

### Step 11: screen-spec.md — UI 仕様追記
- [x] `/resources` の フィルタフォーム仕様にキーワード入力欄を追記する
- **場所**: `Docs/spec/screen-spec.md`

---

## 重要な実装注意事項

1. **既存テストの互換性**: `ResourceServiceTest` の既存 4 テストは stub が変わるが、assertion は同じ。`list(null, null, null, false, pageable)` → `list(null, null, null, false, null, pageable)` に更新する
2. **makeResource の description 追加**: `ResourceServiceTest.makeResource()` に `description` フィールドのリフレクション設定を追加する（PBT で使用）
3. **JPQL の `:keyword IS NULL` チェック**: keyword=null のとき WHERE 句が空になるよう JPQL で `IS NULL` チェックを組み込む
4. **Locale.ROOT**: Java Stream の `toLowerCase()` は `Locale.ROOT` を使用する
5. **Spotless/Checkstyle**: Google Java Format に従う（既存コードのスタイルを維持）

---

## 受入条件（コード生成後の確認）

- [ ] 既存テスト（`ResourceServiceTest`・`ResourceControllerTest`）が pass する
- [ ] keyword 検索テストが pass する
- [ ] jqwik PBT（P-01〜P-05）が pass する
- [ ] keyword=null のとき全件が返される（後方互換）
- [ ] JPQL は `:keyword` パラメータバインドを使用する（SECURITY-05）
