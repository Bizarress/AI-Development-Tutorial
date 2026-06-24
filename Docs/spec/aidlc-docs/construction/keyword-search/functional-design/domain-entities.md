---
type: functional-design
artifact: domain-entities
unit: keyword-search
timestamp: 2026-06-23T00:00:00Z
---

# ドメインエンティティ — keyword-search

## 変更対象エンティティ

### Resource（既存・読み取り専用変更）

| フィールド | 型 | 制約 | 本エンハンスでの役割 |
|---|---|---|---|
| `id` | UUID | NOT NULL | — |
| `name` | String | NOT NULL, length=100 | keyword 検索の対象①（DB/Java 両パス） |
| `category` | ResourceCategory | NOT NULL | 既存フィルタ（変更なし） |
| `capacity` | Integer | nullable | — |
| `location` | String | length=200, nullable | 検索対象外 |
| `requiresApproval` | boolean | NOT NULL | — |
| `isActive` | boolean | NOT NULL | ADMIN 判定フィルタ（変更なし） |
| **`description`** | **String** | **TEXT, nullable** | **keyword 検索の対象②（DB/Java 両パス）** |
| `createdAt` | LocalDateTime | NOT NULL | — |

**注**: DB スキーマ変更なし。既存の `name` と `description` カラムを検索対象として利用するのみ。

---

## 変更対象コンポーネント（メソッドシグネチャ）

### ResourceController

```java
// 変更前
public Page<ResourceResponse> list(
    @RequestParam(required = false) ResourceCategory category,
    @RequestParam(required = false) LocalDateTime from,
    @RequestParam(required = false) LocalDateTime to,
    @PageableDefault(size = 20) Pageable pageable,
    @CurrentUser User currentUser)

// 変更後（keyword 追加）
public Page<ResourceResponse> list(
    @RequestParam(required = false) ResourceCategory category,
    @RequestParam(required = false) LocalDateTime from,
    @RequestParam(required = false) LocalDateTime to,
    @RequestParam(required = false) String keyword,       // 追加
    @PageableDefault(size = 20) Pageable pageable,
    @CurrentUser User currentUser)
```

### ResourceService

```java
// 変更前
public Page<ResourceResponse> list(
    ResourceCategory category, LocalDateTime from, LocalDateTime to,
    boolean isAdmin, Pageable pageable)

// 変更後（keyword 追加）
public Page<ResourceResponse> list(
    ResourceCategory category, LocalDateTime from, LocalDateTime to,
    boolean isAdmin, String keyword, Pageable pageable)

// listPaginated（keyword 追加）
private Page<ResourceResponse> listPaginated(
    ResourceCategory category, boolean isAdmin, String keyword, Pageable pageable)

// listWithAvailabilityFilter（keyword 追加）
private Page<ResourceResponse> listWithAvailabilityFilter(
    ResourceCategory category, LocalDateTime from, LocalDateTime to,
    boolean isAdmin, String keyword, Pageable pageable)
```

### ResourceRepository

```java
// 追加メソッド①: ページネーション用（listPaginated で使用）
@Query("SELECT r FROM Resource r " +
       "WHERE (:isAdmin = true OR r.isActive = true) " +
       "AND (:category IS NULL OR r.category = :category) " +
       "AND (:keyword IS NULL " +
       "     OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
       "     OR (r.description IS NOT NULL " +
       "         AND LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
Page<Resource> findWithFilters(
    @Param("isAdmin") boolean isAdmin,
    @Param("category") ResourceCategory category,
    @Param("keyword") String keyword,
    Pageable pageable);

// 追加メソッド②: 全件取得用（fetchAllCandidates で使用）
@Query("SELECT r FROM Resource r " +
       "WHERE (:isAdmin = true OR r.isActive = true) " +
       "AND (:category IS NULL OR r.category = :category)")
List<Resource> findAllCandidates(
    @Param("isAdmin") boolean isAdmin,
    @Param("category") ResourceCategory category);
```

> **既存メソッドは削除しない**: `findByIsActiveTrue()` 等の既存メソッドは他のコードから参照される可能性があるため残置。`listPaginated` は新規 `findWithFilters` に切り替える。
