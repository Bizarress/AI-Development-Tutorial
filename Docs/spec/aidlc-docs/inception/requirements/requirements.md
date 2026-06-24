---
type: requirements
title: Requirements — リソース一覧のソート順選択
description: resource-list-sort エンハンスの要件定義（AI-DLC Requirements Analysis 成果物）
tags: [ai-dlc, requirements, resource-list-sort]
timestamp: 2026-06-24
---

# 要件定義 — リソース一覧のソート順選択

## Intent Analysis

| 項目 | 内容 |
|---|---|
| **ユーザーリクエスト** | `resource-list-sort.md` に基づくソート選択機能の追加 |
| **Request Type** | Enhancement（既存リソース一覧機能の改善） |
| **スコープ** | Multiple Components（バックエンド 2ファイル + フロントエンド 1ファイル） |
| **複雑度** | Simple（Spring Data `Pageable`/`Sort` 活用で最小実装） |

---

## Functional Requirements

### FR-01 — バックエンド: sort クエリパラメータの追加

**内容**: `GET /api/resources` に `sort` クエリパラメータを追加する。Spring Data 標準形式（`sort=field,direction`）を採用し、`PageableHandlerMethodArgumentResolver` がそのまま解釈できるようにする。

| 対応フィールド | 方向 | 例 |
|---|---|---|
| `name` | `asc` / `desc` | `sort=name,asc` |
| `capacity` | `asc` / `desc` | `sort=capacity,desc` |
| `createdAt` | `asc` / `desc` | `sort=createdAt,asc` |

- `sort` 未指定時のデフォルトは `createdAt,asc`（既存動作を維持）
- 無効なフィールド名・方向が指定された場合はデフォルトにフォールバック（エラーにしない）

### FR-02 — バックエンド: 既存2ルートへのソート適用方式

**内容**: 既存の2ルート（`listPaginated` / `listWithAvailabilityFilter`）に `sort` を組み込む。

| ルート | 条件 | ソート適用方式 |
|---|---|---|
| `listPaginated` | `from`/`to` なし | DB レベル — `Pageable` に `Sort` を含めて Repository に渡す |
| `listWithAvailabilityFilter` | `from`/`to` あり | Java レベル — `fetchAllCandidates` 後に `Comparator` でソート |

### FR-03 — フロントエンド: ソート選択 UI の追加

**内容**: `ResourceFilterForm.tsx` に shadcn/ui `<Select>` コンポーネントでソート選択 UI を追加する。

- 選択値を URL パラメータ `sort=field,direction` として付与（Q1=A: 1つのセレクト）
- 選択肢例: 「登録日時（新しい順）」「登録日時（古い順）」「名称（昇順）」「名称（降順）」「定員（多い順）」「定員（少ない順）」
- ソート未選択時は `sort` パラメータを URL に含めない（デフォルト動作を維持）
- リセット時は `sort` もクリア（既存の `router.push("/resources")` で対応）

---

## Non-Functional Requirements

### NFR-01: テスト追加（Q3=B: BE+FE）

- **バックエンド**: `ResourceServiceTest` にソートケースを追加（name/capacity/createdAt × asc/desc）
- **フロントエンド**: `resources.test.ts` に Server Action の sort パラメータ引き回しテストを追加

### NFR-02: Security Baseline（Q4=A: 全適用）

本エンハンスは既存認証・認可フロー（JWT + Spring Security）を変更しない。適用されるルールを特定する。

- SECURITY-05（入力バリデーション）: sort パラメータの許容値を明示的に制限する（Spring Data の `Sort` に渡す前に許可フィールドを検証）
- SECURITY-08（エラーレスポンス）: 無効な sort パラメータでスタックトレースを返さない
- その他のルール（SECURITY-01/02/03/04/06/07/09/10 等）: 本エンハンスのスコープ外または既存設計で対応済み（N/A）

### NFR-03: Property-Based Testing（Q5=A: 全適用）

本エンハンスのソートロジックに識別されたテスタブルプロパティ:

| プロパティ種別 | 対象コンポーネント | 内容 |
|---|---|---|
| **Invariant** (PBT-03) | `listPaginated` ソートロジック | ソート後の結果セットは元の全件数と同じ件数を持つ（要素が増減しない） |
| **Invariant** (PBT-03) | `listPaginated` ソートロジック | 結果セットの隣接要素は指定フィールド・方向の順序関係を満たす |
| **Idempotence** (PBT-04) | sort パラメータ解釈 | 同じ `sort=field,dir` を2回適用しても1回と同じ結果 |

---

## Acceptance Criteria

- [ ] 名称順（昇順・降順）でリソース一覧を並び替えられる
- [ ] 定員順（昇順・降順）でリソース一覧を並び替えられる
- [ ] ソート未選択時は従来どおり登録日時昇順で表示される
- [ ] カテゴリ・期間フィルタ・キーワード検索との組み合わせでもソートが適用される
- [ ] バックエンドの既存テストが引き続き pass する
- [ ] 追加した sort ロジックに対応するテストをバックエンド・フロントエンド両方に追加する

---

## 影響範囲

| ファイル | 変更種別 |
|---|---|
| `backend/.../presentation/ResourceController.java` | `sort` パラメータ受け取り（`@RequestParam` または `Pageable` 拡張） |
| `backend/.../application/ResourceService.java` | sort 分岐・`listWithAvailabilityFilter` の Java ソート |
| `frontend/.../resources/ResourceFilterForm.tsx` | shadcn/ui `<Select>` でソート選択 UI 追加 |
| `frontend/src/server/actions/resources.ts` | `sort` パラメータを API リクエストに追加 |
| `backend/.../ResourceServiceTest.java` | ソートケース追加 |
| `frontend/tests/unit/server/actions/resources.test.ts` | sort 引き回しテスト追加 |
| `Docs/spec/api-spec.md` | `GET /api/resources` — `sort` クエリパラメータ追記 |
| `Docs/spec/screen-spec.md` | `/resources` — ソート選択 UI 追記 |
