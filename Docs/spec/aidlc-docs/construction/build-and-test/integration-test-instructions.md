---
type: integration-test-instructions
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# 統合テスト手順 — keyword-search

## 概要

`ResourceControllerTest` は Spring Boot Test（H2 インメモリ DB）を使った統合テストです。
`keyword` パラメータの End-to-End 動作（Controller → Service → Repository → DB）を確認します。

---

## バックエンド統合テスト実行

```bash
cd /workspace/backend
./gradlew test --tests "com.example.bookflow.presentation.ResourceControllerTest"
```

### 追加シナリオ（keyword 関連）

| テスト | 確認内容 |
|---|---|
| `list_withKeyword_returnsMatchingResourcesOnly` | "会議室" で検索 → `第1会議室` がヒット |
| `list_withKeywordNoMatch_returnsEmptyContent` | 存在しないキーワード → `totalElements=0` |

---

## 手動確認手順（アプリ起動時）

アプリケーションが起動している場合、以下の curl で動作確認できます:

```bash
# keyword なし（全件）
curl -H "Authorization: Bearer <JWT>" \
  "http://localhost:8080/api/resources"

# keyword あり
curl -H "Authorization: Bearer <JWT>" \
  "http://localhost:8080/api/resources?keyword=会議室"

# keyword + category の AND 条件
curl -H "Authorization: Bearer <JWT>" \
  "http://localhost:8080/api/resources?keyword=会議&category=ROOM"

# keyword + from/to の AND 条件
curl -H "Authorization: Bearer <JWT>" \
  "http://localhost:8080/api/resources?keyword=会議室&from=2025-06-01T09:00:00&to=2025-06-01T18:00:00"

# keyword 空文字（フィルタ解除・全件）
curl -H "Authorization: Bearer <JWT>" \
  "http://localhost:8080/api/resources?keyword="
```

### 期待レスポンス

```json
{
  "content": [
    { "id": "...", "name": "第1会議室", ... }
  ],
  "totalElements": 1,
  ...
}
```

---

## フロントエンド手動確認

1. `/resources` ページを開く
2. キーワード入力欄が表示されることを確認（4列グリッド）
3. "会議室" と入力して「絞り込む」をクリック
4. URL が `?keyword=会議室` に変わり、マッチするリソースのみ表示されることを確認
5. キーワードを消して「絞り込む」→ 全件表示に戻ることを確認
6. 「リセット」ボタン → キーワードがクリアされることを確認
