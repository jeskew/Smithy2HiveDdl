package io.jsq.smithy.hiveddl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.IoUtils;

class HiveDdlConverterPluginTest {
    @Test
    void generatesFilesForTargets() {
        String resource = getClass().getResource("ecs_smithy.json").toString();
        Model model = Model.assembler()
                .addUnparsedModel(resource, IoUtils.toUtf8String(getClass().getResourceAsStream("ecs_smithy.json")))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .model(model)
                .fileManifest(manifest)
                .settings(Node.objectNodeBuilder()
                        .withMember("targets", Node.objectNodeBuilder()
                                .withMember("ecs_hiveDdl", "elastic.ecs#Record")
                                .build())
                        .build())
                .build();

        new HiveDdlConverterPlugin().execute(context);

        Assertions.assertTrue(manifest.hasFile("ecs_hiveDdl.json"));
        Assertions.assertEquals(
                Node.parse(IoUtils.toUtf8String(getClass().getResourceAsStream("ecs_hiveDdl.json"))),
                Node.parse(manifest.getFileString("ecs_hiveDdl.json").get()));
    }
}
