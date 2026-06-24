---
type: tech-stack-decisions
title: テックスタック決定 — リソース一覧のソート順選択
unit: resource-list-sort
timestamp: 2026-06-24T10:35:00Z
---

# テックスタック決定 — resource-list-sort

## バックエンド

| 技術 | 選定理由 | バージョン |
|---|---|---|
| Spring Data JPA `Sort` | `PageableHandlerMethodArgumentResolver` が `sort=field,direction` 形式を標準解釈。最小実装でソート対応可能 | 既存（Spring Boot 4.0.6 同梱） |
| Java `Comparator<T>` | `listWithAvailabilityFilter` ルートでの Java レベルソートに使用。標準ライブラリのみで外部依存不要 | 既存（Java 25） |
| jqwik | PBT-09 準拠。JUnit 5 統合・shrinking・seed 再現性をサポート。`ResourceSortPropertyTest` で Invariant/Idempotence を実装 | 1.9.1（`build.gradle.kts` 追加済み） |

### jqwik 依存関係確認

```kotlin
// backend/build.gradle.kts（既存）
testImplementation("net.jqwik:jqwik:1.9.1")
```

追加の依存関係変更は不要。

## フロントエンド

| 技術 | 選定理由 | バージョン |
|---|---|---|
| shadcn/ui `<Select>` | Q1=A 回答。既存コンポーネントセットで利用可能。`onValueChange` で直接 URL パラメータ更新が可能 | 既存（プロジェクト設定済み） |
| `useRouter` (Next.js) | Sort 選択時の URL 更新に使用。既存の keyword/category フィルタと同じパターン | 既存（Next.js 15） |
| fast-check | 今回は **不採用**。FE の sort 機能は UI wiring のみ（値を URL に付与するだけ）で算術・変換ロジックがなく、PBT-01 で N/A と判定 | — |

## 新規追加なし

本エンハンスは既存のテックスタック内で完結する。新規ライブラリ・インフラコンポーネントの追加はない。

## SECURITY-05 実装アプローチ

| アプローチ | 選定理由 |
|---|---|
| Java `Set.of("name", "capacity", "createdAt")` による許可リスト検証 | Spring Data `Sort` に渡す前にアプリレベルで検証。追加ライブラリ不要 |
| `ResourceService` 内に `parseSortParam()` として実装 | Controller を薄く保ち、Service がロジックを担当するレイヤー規約に準拠 |
