---
type: nfr-requirements
title: NFR 要件 — リソース一覧のソート順選択
unit: resource-list-sort
timestamp: 2026-06-24T10:35:00Z
---

# NFR 要件 — resource-list-sort

## Security Compliance（Security Baseline Full）

各ルールを本エンハンスのスコープで評価する。

| ルール ID | 適用 | 評価 | 内容 |
|---|---|---|---|
| SECURITY-01 | N/A | — | 新規データストアなし |
| SECURITY-02 | N/A | — | ネットワーク中継設定変更なし |
| SECURITY-03 | N/A | — | 既存ロギング（SLF4J）で対応済み。sort パラメータはログ可（PII なし） |
| SECURITY-04 | N/A | — | HTTP セキュリティヘッダーは既存 Next.js ミドルウェアが担当 |
| **SECURITY-05** | **YES** | **要実装** | `sort` パラメータのフィールド部分を `{name, capacity, createdAt}` の許可リストで検証する。許可リスト外の文字列は `createdAt,asc` にフォールバックし、Spring Data `Sort` に渡す前に検証済みの値のみを使用する |
| SECURITY-06 | N/A | — | IAM ポリシー変更なし |
| SECURITY-07 | N/A | — | ネットワーク構成変更なし |
| SECURITY-08 | CONFIRM | COMPLIANT | 既存の Spring Security + JWT が `/api/resources` を保護済み。sort パラメータ追加で認証・認可機構は変更しない |
| SECURITY-09 | YES | COMPLIANT | 無効な sort パラメータでは HTTP 4xx を返さず、デフォルト値にサイレントフォールバック（BR-05）。Spring Boot の `server.error.include-stacktrace=never` 設定により既存でスタックトレース非公開 |
| SECURITY-10 | N/A | — | 新規依存関係なし（Spring Data・jqwik は既存） |
| SECURITY-11 | N/A | — | レート制限は既存設計で対応済み |
| SECURITY-12 | N/A | — | 認証機構変更なし |
| SECURITY-13 | N/A | — | 新規デシリアライズなし |
| SECURITY-14 | N/A | — | 監視・アラート設定変更なし |
| SECURITY-15 | YES | 要実装 | sort パラメータ解析でフォールバックを安全に実装。例外を握りつぶさず適切に処理する |

### SECURITY-05 実装要件（ブロッキング制約）

```java
// 許可フィールドセット
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "capacity", "createdAt");
// 許可方向セット
private static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc", "desc");

// sort パラメータ → SortParam 変換（Spring Data Sort に渡す前に検証）
Sort parseSortParam(String sortParam) {
    if (sortParam == null || sortParam.isBlank()) {
        return Sort.by(Sort.Direction.ASC, "createdAt"); // DEFAULT
    }
    String[] parts = sortParam.split(",", 2);
    String field = parts[0];
    String direction = parts.length > 1 ? parts[1].toLowerCase() : "asc";

    if (!ALLOWED_SORT_FIELDS.contains(field)) {
        return Sort.by(Sort.Direction.ASC, "createdAt"); // フォールバック
    }
    if (!ALLOWED_DIRECTIONS.contains(direction)) {
        direction = "asc"; // 方向のみ修正
    }
    return Sort.by(Sort.Direction.fromString(direction), field);
}
```

---

## PBT Compliance（Property-Based Testing Full）

| ルール ID | 適用 | 評価 | 内容 |
|---|---|---|---|
| **PBT-01** | YES | COMPLIANT | Functional Design で P-01（件数不変）・P-02（順序関係）・P-03（Idempotence）を識別済み |
| PBT-02 | N/A | — | sort は可逆ではない（Round-trip プロパティなし） |
| **PBT-03** | YES | 要実装 | P-01（件数不変）・P-02（順序関係）を jqwik で実装する |
| **PBT-04** | YES | 要実装 | P-03（同一 sort 2回適用 = 1回と同じ結果）を jqwik で実装する |
| PBT-05 | N/A | — | Spring Data Sort が参照実装として機能するため oracle テスト不要 |
| PBT-06 | N/A | — | stateful コンポーネントなし（HTTP リクエスト単位の純粋変換） |
| **PBT-07** | YES | 要実装 | `Resource` ドメインオブジェクトの jqwik Arbitraries ジェネレータが必要（name/capacity/createdAt の有効値範囲） |
| **PBT-08** | YES | COMPLIANT | jqwik はデフォルトで shrinking をサポート。`@Property(seed=...)` で再現可能 |
| **PBT-09** | YES | COMPLIANT | jqwik 1.9.1（`build.gradle.kts` に追加済み）。FE は sort 引き回し（UI wiring）のみのため fast-check は N/A |
| **PBT-10** | YES | 要実装 | 既存の example-based `ResourceServiceTest` に PBT テストを追加（補完関係）。ビジネスクリティカルパスに example-based テストも必須 |

### PBT 実装要件

**テストクラス**: `ResourceSortPropertyTest.java`（`ResourceServiceTest.java` とは分離して管理）

| プロパティ | jqwik アノテーション | 検証内容 |
|---|---|---|
| P-01: 件数不変 | `@Property` | `|sort(resources, param)| == |resources|` |
| P-02: 順序関係 | `@Property` | `∀ i<j: comparator.compare(sorted[i], sorted[j]) ≤ 0` |
| P-03: Idempotence | `@Property` | `sort(sort(list, p), p) == sort(list, p)` |

**ジェネレータ要件**（PBT-07 準拠）:

```java
@Provide
Arbitrary<Resource> resources() {
    return Combinators.combine(
        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100), // name
        Arbitraries.integers().between(1, 500),                          // capacity
        Arbitraries.longs().between(0, System.currentTimeMillis())       // createdAt (epoch)
    ).as((name, capacity, epochMs) -> {
        Resource r = new Resource();
        r.setName(name);
        r.setCapacity(capacity);
        r.setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneOffset.UTC));
        return r;
    });
}
```

---

## その他 NFR

| NFR | 内容 | 評価 |
|---|---|---|
| パフォーマンス | sort はインデックスを活用した DB ORDER BY または Java Stream Comparator。大量データでも既存クエリと同程度の性能を維持 | 既存設計で対応 |
| 後方互換性 | sort パラメータ未指定時は既存の `createdAt,asc` 動作を維持（BR-03） | 設計済み |
| テスト網羅性 | BE: ResourceServiceTest（example-based） + ResourceSortPropertyTest（PBT）。FE: resources.test.ts（example-based） | 要実装 |
