package dev.send.api.catalog.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public record NodeIoCatalogDto(List<NodeIoDefinitionDto> nodes) {

  public record NodeIoDefinitionDto(
      String nodeType,
      String displayName,
      String nodeClass,
      String theme,
      List<NodeIoPortDto> inputs,
      List<NodeIoPortDto> outputs,
      List<NodeIoDataFieldDto> dataFields) {}

  public record NodeIoPortDto(
      int index, String name, String arity, String valueType, String valueTypeClass) {}

  public record NodeIoDataFieldDto(
      String name,
      @Nullable String label,
      String valueType,
      String valueTypeClass,
      boolean required,
      @Nullable JsonNode defaultValue,
      List<String> options,
      List<String> readableOptions,
      @Nullable NodeIoDataFieldDependencyDto optionFilter) {}

  public record NodeIoDataFieldDependencyDto(
      String field, Map<String, NodeIoDataFieldOptionsDto> groups) {}

  public record NodeIoDataFieldOptionsDto(List<String> options, List<String> readableOptions) {}
}
