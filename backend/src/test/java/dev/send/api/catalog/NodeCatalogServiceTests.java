package dev.send.api.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.send.api.catalog.api.dto.NodeIoCatalogDto;
import dev.send.api.catalog.application.NodeCatalogService;
import dev.send.api.catalog.application.NodeSpecCatalogLoader;
import dev.send.api.catalog.application.NodeSpecSetConfig;
import dev.send.api.catalog.domain.NodeSpecSet;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class NodeCatalogServiceTests {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void setBuilderRegistersPrimitiveFetchAndDerivedSets() {
    List<NodeSpecSetConfig> configs =
        new NodeSpecSetConfig.Builder()
            .addSet(NodeSpecSet.PRIMITIVE, "node-specs/primitive")
            .addSet(NodeSpecSet.FETCH, "node-specs/fetch")
            .addSet(NodeSpecSet.DERIVED, "node-specs/derived")
            .build();

    assertEquals(3, configs.size());
    assertEquals(NodeSpecSet.PRIMITIVE, configs.get(0).set());
    assertEquals("node-specs/fetch", configs.get(1).resourceSubdirectory());
    assertEquals(NodeSpecSet.DERIVED, configs.get(2).set());
  }

  @Test
  void loaderUsesRegisteredSetConfigInsteadOfHardcodedFileNames() {
    NodeSpecCatalogLoader loader = new NodeSpecCatalogLoader(objectMapper);

    assertIterableEquals(
        List.of(NodeSpecSet.PRIMITIVE, NodeSpecSet.FETCH, NodeSpecSet.DERIVED),
        loader.registeredSets().stream().map(NodeSpecSetConfig::set).toList());
    assertIterableEquals(
        List.of("node-specs/primitive", "node-specs/fetch", "node-specs/derived"),
        loader.registeredSets().stream().map(NodeSpecSetConfig::resourceSubdirectory).toList());
  }

  @Test
  void catalogLoadsNodeSpecsAcrossSetsAndProjectsNodeIoShape() {
    NodeCatalogService nodeCatalogService =
        new NodeCatalogService(new NodeSpecCatalogLoader(objectMapper));

    assertEquals(3, nodeCatalogService.registeredSets().size());
    assertTrue(nodeCatalogService.findSpec("add").isPresent());
    assertTrue(nodeCatalogService.findSpec("fetch_price").isPresent());
    assertTrue(nodeCatalogService.findSpec("average").isPresent());

    NodeIoCatalogDto catalog = nodeCatalogService.getNodeIoCatalog();
    assertTrue(catalog.nodes().stream().anyMatch(node -> node.nodeType().equals("fetch_price")));
    assertTrue(catalog.nodes().stream().anyMatch(node -> node.nodeClass().equals("FETCH")));
    assertEquals(
        "market",
        catalog.nodes().stream()
            .filter(node -> node.nodeType().equals("fetch_price"))
            .findFirst()
            .orElseThrow()
            .theme());
    assertEquals(
        "Fetch Price Data",
        catalog.nodes().stream()
            .filter(node -> node.nodeType().equals("fetch_price"))
            .findFirst()
            .orElseThrow()
            .displayName());
    assertEquals(
        "NumVal",
        catalog.nodes().stream()
            .filter(node -> node.nodeType().equals("add"))
            .findFirst()
            .orElseThrow()
            .outputs()
            .getFirst()
            .valueTypeClass());
    NodeIoCatalogDto.NodeIoDefinitionDto average =
        catalog.nodes().stream().filter(node -> node.nodeType().equals("average")).findFirst().orElseThrow();
    assertEquals("Average", average.displayName());
    assertEquals("MANY", average.inputs().getFirst().arity());
    assertEquals("values", average.inputs().getFirst().name());
    assertEquals(
        "ticker",
        catalog.nodes().stream()
            .filter(node -> node.nodeType().equals("fetch_price"))
            .findFirst()
            .orElseThrow()
            .dataFields()
            .getFirst()
            .name());

    NodeIoCatalogDto.NodeIoDefinitionDto fetchFinancialStatements =
        catalog.nodes().stream()
            .filter(node -> node.nodeType().equals("fetch_financial_statements"))
            .findFirst()
            .orElseThrow();

    NodeIoCatalogDto.NodeIoDataFieldDto sourceDataset =
        fetchFinancialStatements.dataFields().stream()
            .filter(field -> field.name().equals("sourceDataset"))
            .findFirst()
            .orElseThrow();
    assertEquals("Financial Statement", sourceDataset.label());
    assertEquals(
        List.of("us-income-quarterly", "us-balance-quarterly", "us-cashflow-quarterly"),
        sourceDataset.options());
    assertEquals(
        List.of("Income Statement", "Balance Sheet", "Cash Flow Statement"),
        sourceDataset.readableOptions());
    assertNull(sourceDataset.optionFilter());

    NodeIoCatalogDto.NodeIoDataFieldDto statementField =
        fetchFinancialStatements.dataFields().stream()
            .filter(field -> field.name().equals("field"))
            .findFirst()
            .orElseThrow();
    NodeIoCatalogDto.NodeIoDataFieldDependencyDto optionFilter =
        Objects.requireNonNull(statementField.optionFilter());
    assertEquals("sourceDataset", optionFilter.field());
    assertTrue(optionFilter.groups().containsKey("us-income-quarterly"));
    NodeIoCatalogDto.NodeIoDataFieldOptionsDto incomeStatementOptions =
        Objects.requireNonNull(optionFilter.groups().get("us-income-quarterly"));
    assertTrue(
        incomeStatementOptions.options().containsAll(List.of("revenue", "cost_of_revenue", "sga")));
  }
}
