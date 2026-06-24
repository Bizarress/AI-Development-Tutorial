---
type: build-and-test-summary
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# ビルド・テスト サマリー — keyword-search

## 変更ファイルサマリー

| ファイル | 変更種別 | 内容 |
|---|---|---|
| `backend/build.gradle.kts` | 修正 | jqwik 1.9.1 追加（testImplementation） |
| `ResourceRepository.java` | 修正 | `findWithFilters` / `findAllCandidates` 追加 |
| `ResourceService.java` | 修正 | keyword 対応・`normalizeKeyword`・`Locale.ROOT` |
| `ResourceController.java` | 修正 | `@RequestParam keyword` 追加 |
| `ResourceServiceTest.java` | 修正 | stub 更新 + keyword 例ベーステスト 6 件 |
| `ResourceKeywordFilterPropertyTest.java` | **新規** | jqwik PBT（P-01〜P-05） |
| `ResourceControllerTest.java` | 修正 | keyword 統合テスト 2 件 |
| `resources.ts` | 修正 | `keyword` パラメータ追加 |
| `ResourceFilterForm.tsx` | 修正 | keyword 入力欄・4列グリッド |
| `page.tsx` | 修正 | `searchParams.keyword` 追加 |
| `Docs/spec/api-spec.md` | 修正 | `keyword` パラメータ追記 |
| `Docs/spec/screen-spec.md` | 修正 | UI 仕様追記 |

---

## テスト実行チェックリスト

### バックエンド

```bash
cd /workspace/backend
./gradlew spotlessApply test
```

- [ ] `ResourceServiceTest` — 全件 pass（既存テスト + keyword テスト 6 件）
- [ ] `ResourceKeywordFilterPropertyTest` — jqwik PBT P-01〜P-05 pass
- [ ] `ResourceControllerTest` — 全件 pass（既存テスト + keyword テスト 2 件）

### フロントエンド

```bash
cd /workspace/frontend
npx tsc --noEmit
```

- [ ] TypeScript 型チェック pass

---

## セキュリティコンプライアンス確認（SECURITY-05）

- [ ] `ResourceRepository.findWithFilters()` が `:keyword` パラメータバインドを使用している
- [ ] `ResourceService.listWithAvailabilityFilter()` が `String.contains()` で Java Stream フィルタを使用している
- [ ] 文字列連結による JPQL 組み立てがないこと

---

## PBT コンプライアンス確認（PBT-01〜10）

| ルール | 状態 | 内容 |
|---|---|---|
| PBT-01 | 準拠 | Functional Design で P-01〜P-05 を識別済み |
| PBT-02 | N/A | 本エンハンスに round-trip 操作なし |
| PBT-03 | 準拠 | P-01（マッチ完全性）・P-02（単調性）で不変条件を検証 |
| PBT-04 | 準拠 | P-03（冪等性）で検証 |
| PBT-05 | N/A | 参照実装なし |
| PBT-06 | N/A | 状態管理コンポーネントなし |
| PBT-07 | 準拠 | `resources()` / `keywords()` ジェネレータを定義 |
| PBT-08 | 準拠 | jqwik デフォルト shrinking 有効・seed ロギング |
| PBT-09 | 準拠 | jqwik 1.9.1 を testImplementation に追加 |
| PBT-10 | 準拠 | 例ベーステストと PBT を並存（別クラス） |

---

## 受入条件チェック

- [ ] キーワードを入力して絞り込むと、name/description にそのキーワードを含む結果のみが表示される
- [ ] キーワードフィールドを空にして「絞り込む」を押すと、キーワード条件が解除される
- [ ] カテゴリ・期間フィルタとキーワードを同時に指定できる（AND 条件）
- [ ] `keyword` パラメータ未指定時の動作は既存と変わらない
- [ ] `ResourceServiceTest` 等の既存テストが引き続き pass する
- [ ] keyword 検索ロジックに対応するユニットテストをバックエンドに追加した
- [ ] jqwik による PBT を追加した
- [ ] JPQL パラメータバインドにより SQL インジェクションが防止される

---

## 全体ステータス

- **Build**: 手動実行が必要（手順は `build-instructions.md` 参照）
- **Unit Tests**: 手動実行が必要（手順は `unit-test-instructions.md` 参照）
- **Integration Tests**: 手動実行が必要（手順は `integration-test-instructions.md` 参照）
- **Performance Tests**: N/A（学習環境・スコープ外）
- **Operations**: PLACEHOLDER（将来の拡張）
