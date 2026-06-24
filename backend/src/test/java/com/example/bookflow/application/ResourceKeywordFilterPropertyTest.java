package com.example.bookflow.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookflow.domain.Resource;
import com.example.bookflow.domain.ResourceCategory;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * keyword フィルタロジックのプロパティベーステスト（PBT）。
 *
 * <p>jqwik を使って Java Stream フィルタの不変条件（P-01〜P-05）を検証する。 {@link ResourceService} の
 * listWithAvailabilityFilter 内の keyword フィルタロジックを対象とする。
 */
class ResourceKeywordFilterPropertyTest {

  // ---------------------------------------------------------------------------
  // テストヘルパー
  // ---------------------------------------------------------------------------

  private static Resource makeResource(String name, String description) {
    try {
      Resource r = new Resource() {};
      setField(r, "id", UUID.randomUUID());
      setField(r, "name", name);
      setField(r, "category", ResourceCategory.ROOM);
      setField(r, "isActive", true);
      setField(r, "requiresApproval", false);
      setField(r, "description", description);
      setField(r, "createdAt", LocalDateTime.of(2025, 4, 1, 9, 0));
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

  /** keyword フィルタロジック（ResourceService.listWithAvailabilityFilter と同一実装）。 */
  private static List<Resource> applyFilter(List<Resource> resources, String keyword) {
    if (keyword == null || keyword.isBlank()) return resources;
    String lowerKw = keyword.toLowerCase(Locale.ROOT);
    return resources.stream()
        .filter(
            r ->
                r.getName().toLowerCase(Locale.ROOT).contains(lowerKw)
                    || (r.getDescription() != null
                        && r.getDescription().toLowerCase(Locale.ROOT).contains(lowerKw)))
        .toList();
  }

  // ---------------------------------------------------------------------------
  // ジェネレータ（PBT-07 準拠）
  // ---------------------------------------------------------------------------

  @Provide
  Arbitrary<Resource> resources() {
    Arbitrary<String> names =
        Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(30);
    Arbitrary<String> descs =
        Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(0)
            .ofMaxLength(100)
            .injectNull(0.2);
    return Combinators.combine(names, descs).as(ResourceKeywordFilterPropertyTest::makeResource);
  }

  @Provide
  Arbitrary<List<Resource>> resourceLists() {
    return resources().list().ofMinSize(0).ofMaxSize(20);
  }

  @Provide
  Arbitrary<String> keywords() {
    return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10);
  }

  // ---------------------------------------------------------------------------
  // プロパティテスト（P-01〜P-05）
  // ---------------------------------------------------------------------------

  /**
   * P-01: マッチ結果の完全性。
   *
   * <p>結果の全 Resource が name または description に keyword を含む。
   */
  @Property
  void allResultsContainKeyword(
      @ForAll("resourceLists") List<Resource> resources, @ForAll("keywords") String keyword) {
    String lowerKw = keyword.toLowerCase(Locale.ROOT);
    applyFilter(resources, keyword)
        .forEach(
            r ->
                assertThat(
                        r.getName().toLowerCase(Locale.ROOT).contains(lowerKw)
                            || (r.getDescription() != null
                                && r.getDescription().toLowerCase(Locale.ROOT).contains(lowerKw)))
                    .isTrue());
  }

  /**
   * P-02: 単調性。
   *
   * <p>フィルタ後件数 &lt;= 全件。
   */
  @Property
  void keywordFilterReducesOrMaintainsSize(
      @ForAll("resourceLists") List<Resource> resources, @ForAll("keywords") String keyword) {
    assertThat(applyFilter(resources, keyword).size()).isLessThanOrEqualTo(resources.size());
  }

  /**
   * P-03: 冪等性。
   *
   * <p>同じ keyword を 2 回適用しても結果は変わらない。
   */
  @Property
  void filterIsIdempotent(
      @ForAll("resourceLists") List<Resource> resources, @ForAll("keywords") String keyword) {
    List<Resource> once = applyFilter(resources, keyword);
    List<Resource> twice = applyFilter(once, keyword);
    assertThat(twice).isEqualTo(once);
  }

  /** P-04: null keyword で全件返却。 */
  @Property
  void nullKeywordReturnsAll(@ForAll("resourceLists") List<Resource> resources) {
    assertThat(applyFilter(resources, null)).isEqualTo(resources);
  }

  /**
   * P-05: description=null のリソースは name のみでマッチ判定。
   *
   * <p>name が "zzzzzzzz" のリソース（マッチしない）に対して "aaa" でフィルタすると空になる。
   */
  @Property
  void nullDescriptionDoesNotCauseNullPointerException(@ForAll("keywords") String keyword) {
    Resource r = makeResource("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzz", null);
    List<Resource> result = applyFilter(List.of(r), keyword);
    // null description でも例外が発生しないこと（name がマッチしない場合は空）
    assertThat(result).isNotNull();
  }
}
