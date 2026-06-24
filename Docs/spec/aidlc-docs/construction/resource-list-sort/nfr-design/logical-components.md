---
type: nfr-design
title: 論理コンポーネント — リソース一覧のソート順選択
unit: resource-list-sort
timestamp: 2026-06-24T10:45:00Z
---

# 論理コンポーネント — resource-list-sort

## コンポーネント構成

本エンハンスでは新規インフラコンポーネント（キュー・キャッシュ・サーキットブレーカー等）は追加しない。既存コンポーネントへの変更と新規値オブジェクト `SortParam` の追加のみ。

```
+-------------------------------------------------------+
| Frontend (Next.js)                                    |
|                                                       |
|  ResourceFilterForm.tsx                               |
|    + shadcn/ui <Select> (sort 選択 UI)                |
|    + onValueChange → URL params 更新                  |
|                                                       |
|  resources.ts (Server Action)                         |
|    + sort?: string を API リクエストに付与             |
+----------------------------+--------------------------+
                             | HTTP GET /api/resources?sort=...
+----------------------------v--------------------------+
| Backend (Spring Boot)                                 |
|                                                       |
|  ResourceController                                   |
|    + Pageable (with sort via 標準 Resolver)           |
|    + String sort (raw, for 2-route logic)             |
|                                                       |
|  ResourceService                                      |
|    + parseSortParam(sort) → Sort          [新規]      |
|      (SECURITY-05 許可リスト検証)                     |
|    + buildComparator(sortParam)           [新規]      |
|    + listPaginated: Pageable に Sort 含む            |
|    + listWithAvailabilityFilter: Comparator 適用      |
|                                                       |
|  ResourceRepository (変更なし)                        |
|    + findPagedResources: Pageable の Sort を DB に委譲|
|                                                       |
+-------------------------------------------------------+
                             |
+----------------------------v--------------------------+
| Database (PostgreSQL)                                 |
|                                                       |
|  resources テーブル                                   |
|    + ORDER BY (name / capacity / created_at)          |
|    + 既存インデックスを活用                            |
|                                                       |
+-------------------------------------------------------+
```

## 新規論理コンポーネント

### SortParam（値オブジェクト）

| 属性 | 内容 |
|---|---|
| **役割** | sort クエリパラメータの解析結果を型安全に表現 |
| **配置** | `ResourceService` 内 private static メソッド群（または内部クラス） |
| **依存** | なし（純粋な値変換） |
| **テスト** | `ResourceServiceTest`（例題） + `ResourceSortPropertyTest`（PBT P-01〜P-03） |

### ResourceSortPropertyTest（新規テストクラス）

| 属性 | 内容 |
|---|---|
| **役割** | jqwik による PBT: Invariant（P-01/P-02）・Idempotence（P-03）の検証 |
| **配置** | `backend/src/test/java/.../application/ResourceSortPropertyTest.java` |
| **依存** | `jqwik 1.9.1`（既存依存）、`ResourceService`（テスト対象） |
| **分離** | `ResourceServiceTest.java`（例題テスト）とは別クラスで管理（PBT-10 準拠） |

## 既存コンポーネントへの変更サマリー

| コンポーネント | 変更種別 | 内容 |
|---|---|---|
| `ResourceController.java` | Minor | `sort` パラメータ受け取り追加（`@RequestParam`） |
| `ResourceService.java` | Minor | `parseSortParam()` / `buildComparator()` 追加、2ルートに sort 適用 |
| `ResourceFilterForm.tsx` | Minor | shadcn/ui `<Select>` 追加、`onValueChange` ハンドラ実装 |
| `resources.ts` | Minor | `sort?: string` パラメータを API リクエストに追加 |
| `ResourceServiceTest.java` | Minor | ソートケース（例題テスト）追加 |
| `resources.test.ts` | Minor | sort 引き回しテスト追加 |

## インフラ変更なし

| インフラコンポーネント | 状態 |
|---|---|
| PostgreSQL テーブル・スキーマ | 変更なし（Flyway マイグレーション不要） |
| Spring Security 設定 | 変更なし |
| Nginx / ロードバランサ | 変更なし |
| Docker Compose | 変更なし |
| CI/CD パイプライン | 変更なし（既存テストコマンドで対応） |
