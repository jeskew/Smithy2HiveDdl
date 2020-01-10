package io.jsq.smithy.hiveddl;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.IoUtils;

class HiveDdlConverterTest {
    @Test
    void convertsStructureShapeToHiveDdl() {
        String resource = getClass().getResource("ecs_smithy.json").toString();
        Model model = Model.assembler()
                .addUnparsedModel(resource, IoUtils.toUtf8String(getClass().getResourceAsStream("ecs_smithy.json")))
                .assemble()
                .unwrap();
        Assertions.assertEquals(
                Node.parse(IoUtils.toUtf8String(getClass().getResourceAsStream("ecs_hiveDdl.json"))),
                ObjectNode.fromStringMap(new HiveDdlConverter(model)
                        .convertTarget(ShapeId.fromParts("elastic.ecs", "Record"))));
    }
}
