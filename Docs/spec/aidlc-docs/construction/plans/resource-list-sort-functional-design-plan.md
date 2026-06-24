---
type: functional-design-plan
title: Functional Design 計画 — リソース一覧のソート順選択
unit: resource-list-sort
status: In Progress
timestamp: 2026-06-24T10:20:00Z
---

# Functional Design 計画 — resource-list-sort

## 実行ステップ

- [x] Step 1: Unit Context 分析（requirements.md + enhancements/resource-list-sort.md）
- [x] Step 2: 質問不要と判定（Q1〜Q5 の回答で設計方針確定済み）
- [x] Step 3: business-logic-model.md 生成
- [x] Step 4: business-rules.md 生成
- [x] Step 5: domain-entities.md 生成
- [x] Step 6: frontend-components.md 生成

## 設計方針サマリー

| 項目 | 決定内容 |
|---|---|
| sort UI | shadcn/ui `<Select>` 1つ（フィールド+方向 6選択肢） |
| sort パラメータ形式 | `sort=field,direction`（Spring Data 標準） |
| BE ルート1 | `listPaginated`: `Pageable` に `Sort` を含めて DB ソート |
| BE ルート2 | `listWithAvailabilityFilter`: Java `Comparator` で Java ソート |
| フィールド許可リスト | `name`, `capacity`, `createdAt`（SECURITY-05） |
| 無効値フォールバック | 無効フィールド/方向 → `createdAt,asc` |
| PBT プロパティ | Invariant（件数不変・順序関係）、Idempotence（同一 sort 2回） |
