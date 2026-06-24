---
type: nfr-requirements
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# NFR 要件 — keyword-search

## 1. パフォーマンス

| 要件 | 内容 |
|---|---|
| `listPaginated` パス | DB レベル JPQL フィルタ。既存のページネーションと同等のレスポンス |
| `listWithAvailabilityFilter` パス | 全件取得後 Java Stream フィルタ（Q2=B）。大規模データでの非効率は許容（学習環境） |
| DB インデックス | `name` / `description` への LOWER+LIKE は B-tree インデックスの恩恵を受けない。現状は許容（リソース数は限定的と判断）。将来的には全文検索インデックスへの移行を検討 |

---

## 2. セキュリティ（Security Baseline — SECURITY-05 が主対象）

### SECURITY-05: 入力バリデーション・インジェクション防止（APPLICABLE）

- `keyword` は JPQL 名前付きパラメータ（`@Param("keyword")`）でバインドする
- 文字列連結による JPQL 組み立ては**禁止**（コードレビューチェック項目）
- Java Stream 側（`listWithAvailabilityFilter`）は `String.contains()` を使用（SQL リスクなし）
- **実装制約**: `ResourceRepository` の `@Query` アノテーション内で `CONCAT('%', :keyword, '%')` を使用する

### SECURITY-08: アプリケーション層アクセス制御（APPLICABLE — 変更なし）

- `GET /api/resources` は既存の JWT 認証必須ルールを維持する
- `@CurrentUser User currentUser` アノテーションを引き続き使用
- keyword パラメータの追加により認可ロジックの変更は不要

### その他の SECURITY ルール（N/A または既存対応済み）

| ルール | 判定 | 理由 |
|---|---|---|
| SECURITY-01 | N/A | 新規ストレージなし |
| SECURITY-02 | N/A | ネットワーク中継の変更なし |
| SECURITY-03 | N/A | 既存ロギング基盤（logstash-logback-encoder）を使用、変更なし |
| SECURITY-04 | N/A | HTML 配信エンドポイントの変更なし |
| SECURITY-06 | N/A | IAM ポリシーの変更なし |
| SECURITY-07 | N/A | ネットワーク設定の変更なし |
| SECURITY-09 | N/A | エラーレスポンスは既存 GlobalExceptionHandler で処理 |
| SECURITY-10 | Applicable | jqwik を Maven Central から固定バージョンで追加する（後述） |
| SECURITY-11 | N/A | レート制限の新規要件なし（既存設定で対応） |
| SECURITY-12 | N/A | 認証ロジックの変更なし |
| SECURITY-13 | N/A | 信頼されていないデータのデシリアライズなし |
| SECURITY-14 | N/A | 既存のモニタリング設定で対応済み |
| SECURITY-15 | N/A | 既存の GlobalExceptionHandler が対応 |

---

## 3. 信頼性・保守性

| 要件 | 内容 |
|---|---|
| 後方互換 | `keyword = null`（未指定）時の動作は現行と完全一致 |
| 既存テスト | `ResourceServiceTest`・`ResourceControllerTest` がすべて pass すること |
| テスタビリティ | jqwik による PBT（P-01〜P-05）を追加し、不変条件を自動検証 |

---

## 4. プロパティベーステスト（PBT — Q4=A 全ルール適用）

### PBT-09: フレームワーク選択（APPLICABLE）

- **選択フレームワーク**: **jqwik 1.9.1**
- **理由**: JUnit 5 統合（既存テストランナーと共存）、stateful PBT サポート、shrinking・seed 再現性
- **追加先**: `backend/build.gradle.kts` の `testImplementation`
- **ソース**: Maven Central（公式レジストリ、SECURITY-10 準拠）

```kotlin
// build.gradle.kts に追加
testImplementation("net.jqwik:jqwik:1.9.1")
```

### PBT-01: プロパティ識別（APPLICABLE — Functional Design で完了）

Functional Design の `business-logic-model.md` で識別済み（P-01〜P-05）。

### PBT-02〜PBT-08, PBT-10（Code Generation フェーズで実装）

- P-01（Invariant）・P-02（Invariant）・P-03（Idempotence）・P-04〜P-05 を jqwik で実装
- 例ベーステストと PBT を同一テストクラス内に配置（PBT-10）
- shrinking 有効（jqwik デフォルト）、seed ロギング（jqwik の `@Property(seed=...)` で再現可能）

---

## 5. スケーラビリティ

本エンハンスのスコープ外（学習環境のため、現状の直接 JPQL LIKE で許容）。

将来拡張が必要な場合: PostgreSQL の `tsvector` / `pg_trgm` 拡張による全文検索インデックスへの移行を推奨。
