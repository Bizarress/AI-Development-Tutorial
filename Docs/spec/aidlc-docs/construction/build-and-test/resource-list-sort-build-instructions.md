---
type: build-instructions
unit: resource-list-sort
timestamp: 2026-06-24T12:00:00Z
---

# ビルド手順 — resource-list-sort

## 前提条件

| 項目 | 内容 |
|---|---|
| Java | 25（`java -version` で確認） |
| Gradle | ラッパー使用（`./gradlew`） |
| Node.js | 18+ |
| pnpm | 8+（`pnpm -v` で確認） |
| Docker（任意） | ローカル DB が必要な場合 |

---

## バックエンドビルド

```bash
cd /workspace/backend

# フォーマット適用（Spotless）
./gradlew spotlessApply

# コンパイル + Checkstyle（テスト除外）
./gradlew build -x test
```

### 確認ポイント

- `BUILD SUCCESSFUL` が表示されること
- Checkstyle エラーがないこと（`parseSortParam` / `buildComparator` の命名・Javadoc 不備がある場合は修正）
- `ALLOWED_SORT_FIELDS` 定数が `ResourceService.java` の `private static final` として認識されること

---

## フロントエンドビルド

```bash
cd /workspace/frontend

# 依存関係インストール（初回のみ）
pnpm install

# TypeScript 型チェック + Next.js ビルド
pnpm build
```

### 確認ポイント

- TypeScript コンパイルエラーがないこと
  - `ResourceFilterFormProps` に `defaultSort?: string` が追加されていること
  - `SearchParams` に `sort?: string` が追加されていること
- `Route (app)` 一覧に `/resources` が含まれること

---

## トラブルシューティング

### `cannot find symbol: Sort` エラー（BE）

`ResourceService.java` の import に以下が揃っているか確認：

```java
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
```

### `Type 'string | undefined' is not assignable to type 'string'`（FE）

`sort?: string` の undefined 伝播が原因。`sort: params.sort` は `string | undefined` として渡す設計のため、`ListResourcesParams.sort` が `string | undefined` を受け入れることを確認。
