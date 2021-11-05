# query-composer

### example usage

```java
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
class Step {
  @Id
  private Long id;
  private String searchIndex;
  @ManyToOne
  private Order order;
  @ManyToOne
  private Line line;
  private LocalDate date;
  // ...
}
```
```java
import kret11.qc.Search;
import org.springframework.stereotype.Component;
import javax.persistence.EntityManager;

@Component
class SearchSteps extends Search<Step> {

  SearchSteps(EntityManager em) {
    super(em, "select s from Step s", "select count(s) from Step s");
  }

}
```
```java
import kret11.qc.QueryComposer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
class ServiceStepExample {
  
  private SearchSteps search;
  ServiceStepExample(SearchSteps search) {this.search = search;}
  
  Page<Step> pagedSearch(Pageable pageable, String q, List<StepStatus> statuses, LocalDate dateFrom) {
    QueryComposer qc = new QueryComposer(pageable);
    qc.addJoin("join s.order o");
    qc.addJoinFetch("left join fetch s.line fl");
    qc.addCondition("o.finished = false");
    if (StringUtils.isNotBlank(q)) {
      qc.addCondition("lower(s.searchIndex) like :q", Map.of("q", "%" + q.toLowerCase() + "%"));
    }
    if (statuses != null && !statuses.isEmpty()) {
      qc.addCondition("s.status in :statuses", Map.of("statuses", statuses));
    }
    if (dateFrom != null) {
      qc.addCondition("s.date >= :dateFrom", Map.of("dateFrom", dateFrom));
    }
    return search.searchPage(qc);
  }
    
}
```
output:
`[DEBUG] select s from Step s join s.order o left join fetch s.line fl where o.finished = false and lower(s.searchIndex) like '%asdf%' and s.status in ('TODO', 'IN_PROGRESS') and s.date >= '2021-11-05' order by s.id ASC`
