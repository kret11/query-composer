package kret11.qc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Search<T> {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String selectQuery;
  private final String countQuery;
  private final EntityManager em;

  protected Search(EntityManager em, String selectQuery, String countQuery) {
    this.em = em;
    this.selectQuery = selectQuery;
    this.countQuery = countQuery;
  }

  public Stream<T> searchStream(QueryComposer composer) {
    TypedQuery<T> tq = query(composer);
    return tq.getResultStream();
  }

  public Page<T> searchPage(QueryComposer composer) {
    TypedQuery<T> tq = query(composer);
    Long count = countQuery(composer);
    if (count != null) { // it is paged then
      tq.setFirstResult((int) composer.pageable.getOffset());
      tq.setMaxResults(composer.pageable.getPageSize());
    }
    var result = tq.getResultList();
    if (count == null) {
      count = (long)result.size();
    }
    return new PageImpl<T>(result, composer.pageable, count);
  }

  protected TypedQuery<T> query(QueryComposer composer){
    var sort = composer.pageable.getSortOr(Sort.by("id"));
    var conditions = composer.getConditionsQuery();
    var where = !conditions.isEmpty() ? "where " + conditions : "";
    var query = Stream.of(
        this.selectQuery,
        composer.getJoins(),
        composer.getJoinFetch(),
        where,
        "order by " + sort.get().map(o -> "e." + o.getProperty() + " " + o.getDirection()).collect(
            Collectors.joining(", "))
    ).collect(Collectors.joining(" "));
    var tq = em.createQuery(query, entityClass());
    printQuery(query, composer.getParameters());
    composer.getParameters().entrySet().stream()
        .filter(e -> !e.getKey().isBlank())
        .forEach(it -> tq.setParameter(it.getKey(), it.getValue()));
    return tq;
  }

  private Long countQuery(QueryComposer composer) {
    if (composer.pageable.isPaged()) {
      String conditions = composer.getConditionsQuery();
      String joins = "";
      if (composer.getJoins() != null) {
        joins = composer.getJoins();
      }
      var cq = this.countQuery + joins + ((!conditions.isEmpty()) ? " where " + conditions : "");
      var tcq = em.createQuery(cq);
      printQuery(cq, composer.getParameters());
      composer.getParameters().entrySet().stream()
          .filter(e -> !e.getKey().isBlank())
          .forEach(it -> tcq.setParameter(it.getKey(), it.getValue()));
      return (long) tcq.getSingleResult();
    }
    return null;
  }

  private Class<T> entityClass() {
    return (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
        .getActualTypeArguments()[0];
  }

  private void printQuery(String query, Map<String, Object> parameters) {
    String readableQuery = query;
    for (var entry : parameters.entrySet()) {
      readableQuery = readableQuery.replace(":" + entry.getKey(), formatValue(entry.getValue()));
    }
    logger.debug(readableQuery);
  }

  private String formatValue(Object value) {
    if (value == null || value instanceof Number) {
      return Objects.toString(value);
    }
    if (value instanceof Collection) {
      Collection<?> values = (Collection<?>) value;
      return values.stream().map(this::formatValue).collect(Collectors.joining(","));
    }
    return "'" + value + "'";
  }

}
