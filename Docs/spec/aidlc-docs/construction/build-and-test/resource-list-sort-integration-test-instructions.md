---
type: integration-test-instructions
unit: resource-list-sort
timestamp: 2026-06-24T12:00:00Z
---

# 統合テスト手順 — resource-list-sort

## テスト範囲

sort エンハンスの統合テストは以下の 2 レイヤーを対象とする。

| レイヤー | テスト手段 | ポイント |
|---|---|---|
| HTTP → Controller → Service | `@SpringBootTest` + MockMvc | `?sort=name,asc` が BE に到達しソート済み応答を返す |
| Service → DB | H2 インメモリ DB（既存テスト基盤） | `Sort` が `PageRequest` に正しく渡される |

---

## シナリオ 1: GET /api/resources?sort=name,asc

### 目的

`sort` クエリパラメータが Controller → Service → Repository の全経路で正しく伝搬し、ソート済みリストが返ること。

### セットアップ

```bash
# ローカル DB (H2 インメモリ) を使う Spring Boot テストは追加セットアップ不要
cd /workspace/backend
```

### 実行コマンド

```bash
./gradlew test --tests "com.example.bookflow.presentation.ResourceControllerTest*"
```

### 確認ポイント

- HTTP 200 が返ること
- レスポンスの `content[0].name` が name 昇順の先頭であること（アルファベット最小値）
- `sort` を省略した場合も 200 を返し、デフォルト（`createdAt,asc`）でソートされること

---

## シナリオ 2: 不正 sort フィールドのフォールバック（SECURITY-05）

### 目的

許可リスト外のフィールド（例: `password`, `id`, `../../etc/passwd`）が渡されても 400 エラーを返さず、デフォルトソートで正常応答すること（Silent Fallback Pattern）。

### テスト手順（手動確認）

```bash
# Docker 環境でバックエンドが起動している場合
curl -s "http://localhost:8080/api/resources?sort=password,asc" | jq '.content | length'
# → 正常にリストが返ること（400 や 500 ではない）

curl -s "http://localhost:8080/api/resources?sort=../../etc,desc" | jq '.totalElements'
# → 正常にカウントが返ること
```

### 自動テスト確認

`ResourceServiceTest.Sort_.list_invalidSortField_fallsBackToDefault` がユニットレベルで検証済み。

---

## シナリオ 3: from/to + sort の組み合わせ（Java Comparator 経路）

### 目的

`from`/`to` パラメータが指定されると `listWithAvailabilityFilter` が呼ばれ、Java Comparator によるソートが適用されること。

### 実行コマンド

```bash
./gradlew test --tests "com.example.bookflow.application.ResourceServiceTest\$List_\$Sort_.list_sortWithTimeFilter_appliesComparatorSort"
```

### 確認ポイント

- フィルタ後のリストが Comparator でソートされていること
- ページネーション（subList）がソート後に正しく適用されること

---

## フルスタック E2E（Playwright、任意）

### 前提

Docker Compose でフルスタックが起動済みであること：

```bash
docker compose -f /workspace/.devcontainer/docker-compose.yml up -d
```

### 実行コマンド

```bash
cd /workspace/frontend
pnpm test:e2e
```

### 確認ポイント（手動チェック）

1. `/resources` ページを開く
2. 「並び順」ドロップダウンが表示されること（`data-testid="sort-select"`）
3. 「名称（昇順）」を選択して「絞り込む」を押す
4. URL に `?sort=name%2Casc` が付くこと
5. リソースが名称昇順で並ぶこと
6. ページリロード後、ドロップダウンが「名称（昇順）」に復元されること（`defaultSort` props）
