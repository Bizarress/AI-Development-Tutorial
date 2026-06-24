---
type: requirements
title: Requirements — リソース一覧キーワード検索・フィルタ追加
stage: Requirements Analysis
status: Complete
timestamp: 2026-06-23T00:00:00Z
source: Docs/spec/enhancements/resource-list-filter.md + requirement-verification-questions.md
---

# 要件定義 — リソース一覧のキーワード検索・フィルタ追加

## 1. インテント分析

| 項目 | 内容 |
|---|---|
| **ユーザーリクエスト** | `resource-list-filter.md` に基づくキーワード検索機能の追加 |
| **リクエスト種別** | Enhancement（既存機能の拡張） |
| **スコープ** | Multiple Components（バックエンド3ファイル + フロントエンド1ファイル） |
| **複雑度** | Simple（明確な実装パス、影響範囲限定） |
| **関連ユースケース** | UC-02（リソース一覧・空き確認）の拡張 |

---

## 2. 機能要件

### FR-01 — バックエンド: keyword パラメータ追加

**内容**: `GET /api/resources` に `keyword` クエリパラメータを追加する。

- `keyword` が指定されている場合、`resources.name` および `resources.description` への大文字小文字を区別しない部分一致（ILIKE 相当）で絞り込む
- `keyword` が `null` または空文字の場合はフィルタを適用しない（全件対象）
- バリデーション: 長さ制限なし（Q1=B）。`null`・空文字のみ「フィルタなし」として特殊扱い

### FR-02 — バックエンド: 2パスへのキーワード適用方式

**内容**: 既存の2ルートにキーワードフィルタを組み込む方式（Q2=B）。

| ルート | 条件 | キーワード適用方式 |
|---|---|---|
| `listPaginated` | `from`/`to` なし | DB レベル — Repository の JPQL クエリに LOWER(name) LIKE または description に対する ILIKE を追加 |
| `listWithAvailabilityFilter` | `from`/`to` あり | Java レベル — `fetchAllCandidates` 後に `Stream.filter` でキーワードを絞り込む |

> **理由**: `listPaginated` は DB ページネーションを使うため DB レベルが自然。`listWithAvailabilityFilter` は全件取得後 Java フィルタのため Java Stream が一貫性を持つ。Q2=B（シンプル重視）の意図を両パスで具現化。

### FR-03 — バックエンド: 既存フィルタとの AND 条件

**内容**: `category`・`from`/`to`・`keyword` は AND 条件で組み合わせる（RES-04）。

### FR-04 — フロントエンド: キーワード入力フィールドの追加

**内容**: `ResourceFilterForm.tsx` にキーワード入力フィールドを追加する。

- `<Input>` コンポーネントを使用（既存パターンと統一）
- 「絞り込む」送信時に `keyword` を URL パラメータとして付与
- キーワード未入力（空文字）の場合は `keyword` パラメータを URL に含めない
- リセット時はキーワードもクリア（既存の `router.push("/resources")` で対応済み）

---

## 3. 非機能要件

### NFR-01 — 大文字小文字の区別なし

PostgreSQL の ILIKE または `LOWER()` 変換による比較（RES-02）。

### NFR-02 — 後方互換性

`keyword` パラメータ未指定時の動作は既存と変わらない（既存テストがすべて pass すること）。

### NFR-03 — テスト

- バックエンドのユニットテスト（`ResourceServiceTest`）に keyword 検索ロジックを追加
- 既存の `ResourceControllerTest`・`ResourceServiceTest` が引き続き pass すること

---

## 4. セキュリティ要件（Security Baseline — Q3=A 有効）

本エンハンスで特に関連するルール:

| ルール | 内容 | 本エンハンスへの適用 |
|---|---|---|
| **SECURITY-05** | 入力バリデーション・インジェクション防止 | `keyword` パラメータは JPA の JPQL パラメータバインド（`:keyword`）経由で使用し、文字列連結禁止 |
| **SECURITY-08** | アプリケーション層アクセス制御 | `GET /api/resources` は認証必須の既存ルールを維持（変更なし） |
| **SECURITY-09** | エラーハンドリング | 検索ロジックのエラーは既存の GlobalExceptionHandler で処理、スタックトレース非露出 |

その他のルール（SECURITY-01〜04、06〜07、10〜15）は本エンハンスのスコープ外または既存設計で対応済み（N/A）。

---

## 5. プロパティベーステスト要件（PBT — Q4=A 有効）

本エンハンスのキーワード検索ロジックに識別されたテスタブルプロパティ:

| プロパティカテゴリ | 対象 | 内容 |
|---|---|---|
| **Invariant** (PBT-03) | `keyword` フィルタ結果 | 結果セットはすべて `name` または `description` に `keyword` を含む（大文字小文字無視） |
| **Invariant** (PBT-03) | `keyword` なし | `keyword=null` 時の結果 ≥ `keyword` 指定時の結果（単調性） |
| **Idempotence** (PBT-04) | 同一 `keyword` の重複適用 | `filter(filter(list, kw), kw) == filter(list, kw)` |

- PBT フレームワーク: **jqwik**（Java / JUnit 5 統合、PBT-09）
- 例ベーステストと PBT を併用（PBT-10）

---

## 6. 影響範囲

### バックエンド変更対象

| ファイル | 変更内容 |
|---|---|
| `ResourceController.java` | `list()` メソッドに `@RequestParam(required = false) String keyword` 追加、`resourceService.list()` に渡す |
| `ResourceService.java` | `list()` シグネチャに `keyword` 追加、`listPaginated` / `listWithAvailabilityFilter` に振り分け |
| `ResourceRepository.java` | `keyword` 対応の `@Query` メソッド追加（JPQL + LOWER/ILIKE）|
| `ResourceServiceTest.java` | keyword 検索テスト追加（例ベース + PBT）|

### フロントエンド変更対象

| ファイル | 変更内容 |
|---|---|
| `ResourceFilterForm.tsx` | `keyword` 入力フィールド追加、`handleSubmit` に `keyword` パラメータ設定 |
| `ResourceFilterFormProps` | `defaultKeyword?: string` 追加 |
| 親コンポーネント（`page.tsx`） | `searchParams.keyword` を `ResourceFilterForm` に渡す |

### 仕様書更新対象

| ファイル | 更新箇所 |
|---|---|
| `Docs/spec/api-spec.md` | `GET /api/resources` — `keyword` クエリパラメータと挙動を追記 |
| `Docs/spec/screen-spec.md` | `/resources` — フィルタフォームのキーワード入力欄を追記 |

---

## 7. 受入条件

- [ ] キーワードを入力して絞り込むと、リソース名または説明にそのキーワードを含む結果のみが表示される
- [ ] キーワードフィールドを空にして「絞り込む」を押すと、キーワード条件が解除される
- [ ] カテゴリ・期間フィルタとキーワードを同時に指定できる（AND 条件）
- [ ] `keyword` パラメータ未指定時の動作は既存と変わらない
- [ ] `ResourceServiceTest` 等の既存テストが引き続き pass する
- [ ] keyword 検索ロジックに対応するユニットテストをバックエンドに追加する
- [ ] jqwik による PBT が `ResourceServiceTest` に追加される
- [ ] JPQL パラメータバインドにより SQL インジェクションが防止される

---

## 8. 技術的制約・前提

- Spring Boot 4.0.6 / Java 25 / Gradle Kotlin DSL
- `ResourceRepository` は `JpaRepository` を継承（JPA Specification パターンは未使用）
- フロントエンドは Next.js + Shadcn UI（`<Input>`, `<Label>` コンポーネント使用）
- 認証: JWT（全ロール認証必須、既存ルール維持）
