package io.jsq.smithy.hiveddl;

import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.utils.Pair;

final class HiveDdlConverter {
    private final Model model;
    private final ShapeVisitor<String> visitor;

    HiveDdlConverter(Model model) {
        this.model = model;
        visitor = new Visitor(model.getShapeIndex());
    }

    Map<String, String> convertTarget(ShapeId shapeId) {
        return model.getShapeIndex().getShape(shapeId)
                .flatMap(Shape::asStructureShape)
                .orElseThrow(() -> new RuntimeException(shapeId + " not found or is not a structure shape"))
                .getAllMembers().entrySet().stream()
                .map(entry -> new Pair<>(jsonName(entry.getValue()), entry.getValue().accept(visitor)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private static String jsonName(MemberShape memberShape) {
        return memberShape.getTrait(JsonNameTrait.class)
                .map(JsonNameTrait::getValue)
                .orElseGet(memberShape::getMemberName);
    }

    private static final class Visitor extends ShapeVisitor.Default<String> {
        private final ShapeIndex shapeIndex;

        public Visitor(ShapeIndex shapeIndex) {
            this.shapeIndex = shapeIndex;
        }

        @Override
        protected String getDefault(Shape shape) {
            throw new RuntimeException("Shapes of type " + shape.getType() + " cannot be converted to Hive DDL");
        }

        @Override
        public String blobShape(BlobShape shape) {
            return "binary";
        }

        @Override
        public String booleanShape(BooleanShape shape) {
            return "boolean";
        }

        @Override
        public String byteShape(ByteShape shape) {
            return "tinyint";
        }

        @Override
        public String doubleShape(DoubleShape shape) {
            return "double";
        }

        @Override
        public String floatShape(FloatShape shape) {
            return "float";
        }

        @Override
        public String integerShape(IntegerShape shape) {
            return "int";
        }

        @Override
        public String listShape(ListShape shape) {
            return collectionShape(shape);
        }

        @Override
        public String longShape(LongShape shape) {
            return "bigint";
        }

        @Override
        public String mapShape(MapShape shape) {
            return "map<string," + shape.getValue().accept(this) + ">";
        }

        @Override
        public String memberShape(MemberShape shape) {
            return shapeIndex.getShape(shape.getTarget())
                    .orElseThrow(() -> new RuntimeException(shape.getTarget() + " (target of "
                            + shape.toShapeId() + ") could not be found."))
                    .accept(this);
        }

        @Override
        public String setShape(SetShape shape) {
            return collectionShape(shape);
        }

        @Override
        public String shortShape(ShortShape shape) {
            return "smallint";
        }

        @Override
        public String stringShape(StringShape shape) {
            return "string";
        }

        @Override
        public String structureShape(StructureShape shape) {
            return structuredShape(shape.getAllMembers());
        }

        @Override
        public String timestampShape(TimestampShape shape) {
            return "timestamp";
        }

        @Override
        public String unionShape(UnionShape shape) {
            return structuredShape(shape.getAllMembers());
        }

        private String collectionShape(CollectionShape shape) {
            return "array<" + shape.getMember().accept(this) + ">";
        }

        private String structuredShape(Map<String, MemberShape> members) {
            return "struct<" + members.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> jsonName(entry.getValue()) + ":" + entry.getValue().accept(this))
                    .collect(Collectors.joining(",")) + ">";
        }
    }
}
