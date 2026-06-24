package com.example.bookflow.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookflow.domain.Resource;
import com.example.bookflow.domain.ResourceCategory;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.data.domain.Sort;

/**
 * ソートロジックのプロパティベーステスト（PBT）。
 *
 * <p>jqwik を使って sort の不変条件（P-01〜P-03）を検証する。
 * {@link ResourceService#parseSortParam} / buildComparator のロジックを複製して
 * 純粋関数として検証する（PBT-10 準拠: example-based {@link ResourceServiceTest} と分離）。
 */
class ResourceSortPropertyTest {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "capacity", "createdAt");
  private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "createdAt");

  // ---------------------------------------------------------------------------
  // テストヘルパー（ResourceKeywordFilterPropertyTest と同パターン）
  // ---------------------------------------------------------------------------

  private static Resource makeResource(String name, Integer capacity, LocalDateTime createdAt) {
    try {
      Resource r = new Resource() {};
      setField(r, "id", UUID.randomUUID());
      setField(r, "name", name);
      setField(r, "category", ResourceCategory.ROOM);
      setField(r, "isActive", true);
      setField(r, "requiresApproval", false);
      setField(r, "description", null);
      setField(r, "capacity", capacity);
      setField(r, "createdAt", createdAt);
      return r;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void setField(Object obj, String name, Object value) throws Exception {
    Class<?> clazz = obj.getClass().getSuperclass();
    if (clazz == Object.class) clazz = obj.getClass();
    Field field;
    try {
      field = clazz.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      field = clazz.getSuperclass().getDeclaredField(name);
    }
    field.setAccessible(true);
    field.set(obj, value);
  }

  /** parseSortParam のロジック複製（SECURITY-05 許可リスト検証）。 */
  private static Sort parseSortParam(String sortParam) {
    if (sortParam == null || sortParam.isBlank()) return DEFAULT_SORT;
    String[] parts = sortParam.split(",", 2);
    String field = parts[0].trim();
    String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
    if (!ALLOWED_SORT_FIELDS.contains(field)) return DEFAULT_SORT;
    Sort.Direction dir = "desc".equals(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
    return Sort.by(dir, field);
  }

  /** buildComparator のロジック複製（Java ソート用）。 */
  private static Comparator<Resource> buildComparator(Sort sort) {
    if (sort.isUnsorted()) return Comparator.comparing(Resource::getCreatedAt);
    Sort.Order order = sort.iterator().next();
    Comparator<Resource> comparator =
        switch (order.getProperty()) {
          case "name" -> Comparator.comparing(Resource::getName, String.CASE_INSENSITIVE_ORDER);
          case "capacity" ->
              Comparator.comparing(
                  Resource::getCapacity, Comparator.nullsLast(Comparator.naturalOrder()));
          default -> Comparator.comparing(Resource::getCreatedAt);
        };
    return order.isAscending() ? comparator : comparator.reversed();
  }

  /** テスト対象: sort パラメータ文字列を受け取りリストをソートして返す。 */
  private static List<Resource> applySort(List<Resource> resources, String sortParam) {
    Sort sort = parseSortParam(sortParam);
    Comparator<Resource> comparator = buildComparator(sort);
    return resources.stream().sorted(comparator).toList();
  }

  // ---------------------------------------------------------------------------
  // ジェネレータ（PBT-07 準拠: ドメイン制約を反映）
  // ---------------------------------------------------------------------------

  @Provide
  Arbitrary<Resource> resources() {
    Arbitrary<String> names =
        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
    Arbitrary<Integer> capacities =
        Arbitraries.integers().between(1, 500).injectNull(0.1);
    Arbitrary<Long> epochMillis =
        Arbitraries.longs().between(0L, 1_700_000_000_000L);
    return Combinators.combine(names, capacities, epochMillis)
        .as(
            (name, capacity, epoch) -> {
              LocalDateTime createdAt =
                  LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
              return makeResource(name, capacity, createdAt);
            });
  }

  @Provide
  Arbitrary<List<Resource>> resourceLists() {
    return resources().list().ofMinSize(0).ofMaxSize(20);
  }

  @Provide
  Arbitrary<String> sortParams() {
    return Arbitraries.of(
        "name,asc",
        "name,desc",
        "capacity,asc",
        "capacity,desc",
        "createdAt,asc",
        "createdAt,desc");
  }

  // ---------------------------------------------------------------------------
  // プロパティテスト（P-01〜P-03）
  // ---------------------------------------------------------------------------

  /**
   * P-01: Invariant — 件数不変。
   *
   * <p>sort 操作は要素を追加・削除しない。
   */
  @Property
  void sortPreservesCount(
      @ForAll("resourceLists") List<Resource> resources,
      @ForAll("sortParams") String sortParam) {
    assertThat(applySort(resources, sortParam)).hasSize(resources.size());
  }

  /**
   * P-02: Invariant — 順序関係。
   *
   * <p>ソート結果の全隣接ペアが指定フィールド・方向の順序を満たす。
   */
  @Property
  void sortSatisfiesOrdering(
      @ForAll("resourceLists") List<Resource> resources,
      @ForAll("sortParams") String sortParam) {
    List<Resource> sorted = applySort(resources, sortParam);
    Sort sort = parseSortParam(sortParam);
    Comparator<Resource> comparator = buildComparator(sort);
    for (int i = 0; i < sorted.size() - 1; i++) {
      assertThat(comparator.compare(sorted.get(i), sorted.get(i + 1))).isLessThanOrEqualTo(0);
    }
  }

  /**
   * P-03: Idempotence — 同一 sort を 2 回適用しても 1 回と同じ結果。
   *
   * <p>sort(sort(list, p), p) == sort(list, p)
   */
  @Property
  void sortIsIdempotent(
      @ForAll("resourceLists") List<Resource> resources,
      @ForAll("sortParams") String sortParam) {
    List<Resource> once = applySort(resources, sortParam);
    List<Resource> twice = applySort(once, sortParam);
    assertThat(twice).isEqualTo(once);
  }
}
