---
type: tech-stack-decisions
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# テックスタック決定 — keyword-search

## 既存スタック（変更なし）

| レイヤー | 技術 | バージョン |
|---|---|---|
| バックエンド言語 | Java | 25 |
| フレームワーク | Spring Boot | 4.0.6 |
| ORM | Spring Data JPA / Hibernate | Spring Boot BOM 管理 |
| DB | PostgreSQL | 既存（テストは H2） |
| ビルド | Gradle Kotlin DSL | 既存 |
| フロントエンド | Next.js + TypeScript | 既存 |
| UI コンポーネント | Shadcn UI (`<Input>`, `<Label>`) | 既存 |

---

## 新規追加（本エンハンス）

### jqwik（PBT-09）

| 項目 | 内容 |
|---|---|
| ライブラリ | `net.jqwik:jqwik` |
| バージョン | **1.9.1**（固定、SECURITY-10 準拠） |
| スコープ | `testImplementation` |
| ソース | Maven Central（公式レジストリ） |
| 理由 | JUnit 5 統合、shrinking・seed 再現性・stateful PBT サポート |
| 既存テストへの影響 | なし（JUnit 5 との共存が設計上保証されている） |

**build.gradle.kts 追加内容**:
```kotlin
testImplementation("net.jqwik:jqwik:1.9.1")
```

---

## 技術的代替案（採用しなかった理由）

| 代替案 | 採用しなかった理由 |
|---|---|
| JPA Specification（keyword 用） | 既存コードベースが `@Query` パターンを採用しており一貫性を優先。Specification は設定が複雑 |
| Native Query（ILIKE） | JPQL LOWER+LIKE で同等の結果が得られ、DB 非依存性を維持できる |
| Querydsl | 依存関係の追加コストが高く、小規模な変更には不相応 |
| Hypothesis（Python PBT） | バックエンドは Java のため jqwik が適切 |
| fast-check（JavaScript PBT） | バックエンドのテストに不適切（フロントエンドの PBT は本エンハンスでは不要） |

---

## SECURITY-10 コンプライアンス確認

| チェック項目 | 状態 |
|---|---|
| 既存 lock ファイル（`gradle.lockfile`）の有無 | 確認要（build.gradle.kts にロックファイル設定なし） |
| jqwik のバージョン固定 | 1.9.1（固定済み） |
| Maven Central（公式レジストリ）からの取得 | 確認済み |
| 未使用依存関係の排除 | jqwik はテストで使用する（未使用ではない） |

> **注**: `gradle.lockfile` が存在しない場合、バージョン固定は build.gradle.kts の明示的バージョン指定で代替する（学習環境での許容範囲）。
