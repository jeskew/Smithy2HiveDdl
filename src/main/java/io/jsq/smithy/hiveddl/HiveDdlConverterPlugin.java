package io.jsq.smithy.hiveddl;

import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.Pair;

/**
 * Smithy build plugin to convert structure shapes from a model into Hive DDL statements for each member. The output is
 * a JSON file mapping string column names to string DDL statements, formatted for compatibility with AWS Glue.
 */
public final class HiveDdlConverterPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(HiveDdlConverter.class.getName());
    private static final String TARGETS_PROPERTY_NAME = "targets";

    @Override
    public String getName() {
        return "hive-ddl";
    }

    @Override
    public void execute(PluginContext context) {
        context.getSettings().getObjectMember(TARGETS_PROPERTY_NAME)
                .map(ObjectNode::getStringMap)
                .ifPresent(targets -> convertTargets(targets, new HiveDdlConverter(context.getModel()),
                        context.getFileManifest()));
    }

    private void convertTargets(Map<String, Node> targets, HiveDdlConverter converter, FileManifest fileManifest) {
        targets.entrySet().stream()
                .map(entry -> new Pair<>(entry.getKey(), ShapeId.from(entry.getValue().expectStringNode().getValue())))
                .peek(entry -> LOGGER.fine(() -> "Converting " + entry.getRight()
                        + " to Hive DDL as " + entry.getLeft() + ".json"))
                .forEach(entry -> fileManifest.writeJson(entry.getKey() + ".json",
                        ObjectNode.fromStringMap(converter.convertTarget(entry.getRight()))));
    }
}
