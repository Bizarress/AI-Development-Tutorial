---
type: questions
title: 要件確認質問 — リソース一覧のソート順選択
description: resource-list-sort エンハンスの Requirements Analysis 確認質問
tags: [ai-dlc, requirements, questions, resource-list-sort]
timestamp: 2026-06-24
---

# 要件確認質問 — リソース一覧のソート順選択

エンハンス仕様書（`Docs/spec/enhancements/resource-list-sort.md`）を参照し、
以下の点を確認します。各 `[Answer]:` タグに選択肢の文字（A/B/C/X）を記入してください。

---

## Q1: ソート UI のコンポーネント形式

`ResourceFilterForm.tsx` に追加するソート選択 UI の実装形式を選んでください。

A) ドロップダウン（shadcn/ui の `<Select>`）— フィールド＋方向を1つのセレクトで選択（例:「名称 昇順」「名称 降順」「定員 昇順」…）

B) 2つのドロップダウン — フィールド（name/capacity/createdAt）と方向（asc/desc）を分離

C) ラジオボタングループ — 選択肢を並べて表示

X) Other（[Answer]: タグの下に説明を記入）

[Answer]: A

---

## Q2: ソートパラメータの URL 形式

`GET /api/resources` の `sort` クエリパラメータ形式を選んでください。

A) Spring Data 標準形式 `sort=field,direction`（例: `sort=name,asc`）— `PageableHandlerMethodArgumentResolver` がそのまま解釈できる

B) カスタム形式（例: `sortBy=name&sortDir=asc`）— 独自バリデーションが必要

[Answer]: A

---

## Q3: テストの追加範囲

追加するテストのスコープを選んでください。

A) バックエンドのみ（`ResourceServiceTest` にソートケース追加）

B) バックエンド + フロントエンド（`ResourceServiceTest` + `resources.test.ts` に追加）

[Answer]: B

---

## Q4: Security Baseline 拡張

セキュリティ拡張ルールを本エンハンスに適用しますか？

A) Yes — 全 SECURITY ルールをブロッキング制約として適用（本番品質アプリケーションに推奨）

B) No — SECURITY ルールをスキップ（PoC・プロトタイプに適した設定）

X) Other（[Answer]: タグの下に説明を記入）

[Answer]: A

---

## Q5: Property-Based Testing 拡張

プロパティベーステスト（PBT）を本エンハンスに適用しますか？

A) Yes — 全 PBT ルールをブロッキング制約として適用（ビジネスロジック・データ変換に推奨）

B) Partial — 純粋関数のみ PBT を適用

C) No — PBT ルールをスキップ（単純 CRUD・UI のみの場合）

X) Other（[Answer]: タグの下に説明を記入）

[Answer]: A
