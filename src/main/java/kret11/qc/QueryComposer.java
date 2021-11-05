package kret11.qc;

import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryComposer {

  Pageable pageable;
  private final List<String> conditionQueryParts = new ArrayList<>();
  private final Map<String, Object> parameters = new HashMap<>();
  private String joins = "";
  private String joinFetch = "";

  public QueryComposer(Pageable pageable) {
    this.pageable = pageable;
  }

  public void addCondition(String query) {
    conditionQueryParts.add(query);
  }

  public void addCondition(String query, Map<String, Object> param) {
    conditionQueryParts.add(query);
    if (param != null) {
      param.forEach((key, value) -> parameters.put(key, value));
    }
  }

  public String getConditionsQuery() {
    return conditionQueryParts.stream()
        .collect(Collectors.joining(" and "));
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public String getJoins() {
    return joins;
  }

  public String getJoinFetch() {
    return joinFetch;
  }

  public void addJoin(String joins) {
    this.joins += joins + " ";
  }

  public void addJoinFetch(String joins) {
    this.joinFetch += joins + " ";
  }

  public void replaceParameter(Pair<String, String> pair) {
    parameters.put(pair.getFirst(), pair.getSecond());
  }
}
