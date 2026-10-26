package decok.dfcdvadstf.catframe.model.extended.json.transformation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import java.util.Map;

/**
 * BlockstateTransform — ports the Forge model-level {@code transform} field.
 *
 * <p>Forge/NeoForge block models accept a top level {@code transform} entry that
 * applies a global affine transform (translation / rotation / scale) to the
 * whole model, which is how arbitrary-angle rotation is expressed when the
 * blockstate {@code x}/{@code y} fields are not enough. This class reproduces
 * that JSON contract as a {@link Matrix4d} in block space (one block = 1.0,
 * the same space the baked quads live in), so the extension layer of CatFrame
 * can apply it without touching the core pipeline. The JSON is read from the
 * extension-field channel CatFrame keeps for compatibility layers
 * ({@code BlockstateJson.Variant#getRawJson()} and {@code ModelJson#raw}).
 *
 * <p><b>Accepted JSON</b> (mirrors Forge's
 * {@code net.minecraftforge.common.util.TransformationHelper.Deserializer}):
 * <ul>
 *   <li>String {@code "identity"} — no transform.</li>
 *   <li>Array — a 3x4 affine matrix: {@code [[m00,m01,m02,m03],[m10,...],[m20,...]]}.</li>
 *   <li>Object with a sole {@code matrix} key — the same 3x4 array.</li>
 *   <li>Object decomposed into {@code translation}, {@code rotation} (or
 *       {@code left_rotation}), {@code scale}, {@code right_rotation} (or
 *       {@code post-rotation}) and {@code origin}. A rotation tag takes a
 *       4-float quaternion {@code [x,y,z,w]}, a 3-float Euler array in degrees
 *       (applied as X then Y then Z), an array of single-axis objects
 *       ({@code [{"x":90},{"y":45}]}), or a single-axis object ({@code {"y":45}}).
 *       {@code scale} is either a number or a 3-float array.</li>
 * </ul>
 *
 * <p><b>Schema superset</b>: {@code rotation} as an object holding all three of
 * {@code x}/{@code y}/{@code z} is accepted as well; Forge only accepts a
 * single axis per object, so that extra form is for hand-written blockstates.
 *
 * <p><b>Units</b>: {@code translation} and {@code origin} are in block space
 * (0.5 = half a block), not pixels — the same convention as Forge's
 * {@code TransformOrigin} presets ({@code center} = 0.5, {@code corner} = 0,
 * {@code opposing-corner} = 1).
 *
 * <p><b>Pivot quirk kept for parity</b>: Forge defaults {@code origin} to
 * {@code opposing-corner} (1,1,1) when the tag is absent, and rotates/scales
 * around that pivot. This class keeps that default so resource packs authored
 * against Forge render the same way here.
 */
public final class BlockstateTransform {

    /** Forge field name of the model-level transform. */
    public static final String FIELD = "transform";

    /**
     * Alternative field name: CatFrame's compatibility channel documents
     * {@code transformation} (the items JSON name) as its example extension field.
     */
    public static final String LEGACY_FIELD = "transformation";

    /** Epsilon used for identity detection. */
    private static final double IDENTITY_EPSILON = 1.0e-6;

    /** Forge's default pivot: the corner diagonally opposite the model origin. */
    private static final Vector3d DEFAULT_ORIGIN = new Vector3d(1.0, 1.0, 1.0);

    private BlockstateTransform() {
    }

    // ==================== Public API ====================

    /**
     * Parses a {@code transform} JSON element into a block-space matrix.
     *
     * @param element the {@code transform} element, may be null
     * @return the transform matrix; {@code null} when the element is absent or the transform
     *         is the identity (so callers can skip the per-vertex multiplication)
     * @throws JsonParseException when the JSON structure is invalid
     */
    @Nullable
    public static Matrix4d parse(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        Matrix4d matrix;
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            if ("identity".equals(value)) {
                return null;
            }
            throw new JsonParseException(
                    "transform: unknown string form '" + value + "', only \"identity\" is accepted");
        } else if (element.isJsonArray()) {
            matrix = parseMatrix(element.getAsJsonArray());
        } else if (element.isJsonObject()) {
            matrix = parseObject(element.getAsJsonObject());
        } else {
            throw new JsonParseException(
                    "transform must be an object, a 3x4 array or \"identity\", got " + element);
        }

        return isIdentity(matrix) ? null : matrix;
    }

    /**
     * Reads and parses the transform of a raw JSON object (variant or model root).
     *
     * @param json raw JSON object, may be null
     * @return the transform matrix, or {@code null} when no transform field is present
     */
    @Nullable
    public static Matrix4d parseField(@Nullable JsonObject json) {
        if (json == null) {
            return null;
        }
        JsonElement element = json.get(FIELD);
        if (element == null) {
            element = json.get(LEGACY_FIELD);
        }
        return parse(element);
    }

    /**
     * Reads and parses the transform of a model's unrecognized-key snapshot
     * ({@link decok.dfcdvadstf.catframe.model.core.ModelJson#raw}).
     *
     * @param raw unrecognized top-level model keys, may be null
     * @return the transform matrix, or {@code null} when no transform field is present
     */
    @Nullable
    public static Matrix4d parseField(@Nullable Map<String, JsonElement> raw) {
        if (raw == null) {
            return null;
        }
        JsonElement element = raw.get(FIELD);
        if (element == null) {
            element = raw.get(LEGACY_FIELD);
        }
        return parse(element);
    }

    /**
     * Determines whether the matrix equals the identity matrix within tolerance.
     */
    public static boolean isIdentity(Matrix4d m) {
        return Math.abs(m.m00 - 1.0) < IDENTITY_EPSILON
                && Math.abs(m.m11 - 1.0) < IDENTITY_EPSILON
                && Math.abs(m.m22 - 1.0) < IDENTITY_EPSILON
                && Math.abs(m.m33 - 1.0) < IDENTITY_EPSILON
                && Math.abs(m.m01) < IDENTITY_EPSILON && Math.abs(m.m02) < IDENTITY_EPSILON
                && Math.abs(m.m03) < IDENTITY_EPSILON && Math.abs(m.m10) < IDENTITY_EPSILON
                && Math.abs(m.m12) < IDENTITY_EPSILON && Math.abs(m.m13) < IDENTITY_EPSILON
                && Math.abs(m.m20) < IDENTITY_EPSILON && Math.abs(m.m21) < IDENTITY_EPSILON
                && Math.abs(m.m23) < IDENTITY_EPSILON && Math.abs(m.m30) < IDENTITY_EPSILON
                && Math.abs(m.m31) < IDENTITY_EPSILON && Math.abs(m.m32) < IDENTITY_EPSILON;
    }

    // ==================== Decomposed (TRSR) form ====================

    /**
     * Parses the decomposed form: {@code matrix}, or a translation / rotation /
     * scale combination applied as {@code T x L x S x R} (right rotation acts on
     * the vertex first), then re-pivoted around {@code origin}.
     */
    private static Matrix4d parseObject(JsonObject obj) {
        if (obj.has("matrix")) {
            if (obj.entrySet().size() > 1) {
                throw new JsonParseException("transform: 'matrix' cannot be combined with other keys");
            }
            JsonElement matrix = obj.get("matrix");
            if (matrix == null || !matrix.isJsonArray()) {
                throw new JsonParseException("transform: 'matrix' must be a 3x4 array");
            }
            return parseMatrix(matrix.getAsJsonArray());
        }

        Vector3d translation = readVec3(obj.get("translation"), "translation");
        Vector3d scale = readScale(obj.get("scale"));
        Vector3d origin = readOrigin(obj.get("origin"));
        Matrix4d left = parseRotation(obj.has("rotation") ? obj.get("rotation") : obj.get("left_rotation"),
                obj.has("rotation") ? "rotation" : "left_rotation");
        Matrix4d right = parseRotation(obj.has("right_rotation") ? obj.get("right_rotation")
                : obj.get("post-rotation"), "right_rotation");

        // M = T x L x S x R, composed right to left
        Matrix4d linear = new Matrix4d();
        linear.setIdentity();
        if (right != null) {
            linear = compose(right, linear);
        }
        if (scale != null) {
            Matrix4d scaleMatrix = new Matrix4d();
            scaleMatrix.setIdentity();
            scaleMatrix.m00 = scale.x;
            scaleMatrix.m11 = scale.y;
            scaleMatrix.m22 = scale.z;
            linear = compose(scaleMatrix, linear);
        }
        if (left != null) {
            linear = compose(left, linear);
        }

        // Left-multiplying by a pure translation only writes the translation column
        Matrix4d result = new Matrix4d(linear);
        result.m03 = translation != null ? translation.x : 0.0;
        result.m13 = translation != null ? translation.y : 0.0;
        result.m23 = translation != null ? translation.z : 0.0;

        return applyOrigin(result, origin);
    }

    /**
     * Moves the pivot of the transform from the model origin to the given point:
     * {@code M' = Trans(pivot) x M x Trans(-pivot)}, i.e. the linear part stays
     * untouched and the translation absorbs {@code pivot - A x pivot}.
     */
    private static Matrix4d applyOrigin(Matrix4d matrix, Vector3d pivot) {
        if (pivot == null || (pivot.x == 0.0 && pivot.y == 0.0 && pivot.z == 0.0)) {
            return matrix;
        }
        double px = matrix.m00 * pivot.x + matrix.m01 * pivot.y + matrix.m02 * pivot.z;
        double py = matrix.m10 * pivot.x + matrix.m11 * pivot.y + matrix.m12 * pivot.z;
        double pz = matrix.m20 * pivot.x + matrix.m21 * pivot.y + matrix.m22 * pivot.z;

        Matrix4d result = new Matrix4d(matrix);
        result.m03 += pivot.x - px;
        result.m13 += pivot.y - py;
        result.m23 += pivot.z - pz;
        return result;
    }

    // ==================== Rotation ====================

    /**
     * Parses a rotation tag: quaternion array, Euler degree array, array of
     * single-axis objects, or a single-axis object.
     *
     * @return the rotation matrix, or {@code null} when the tag is missing
     */
    @Nullable
    private static Matrix4d parseRotation(@Nullable JsonElement element, String tagName) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() > 0 && array.get(0).isJsonObject()) {
                // [{"x":90},{"y":45}] — multiplied in declaration order
                Matrix4d result = null;
                for (JsonElement axis : array) {
                    Matrix4d step = parseAxisRotation(axis, tagName);
                    if (step == null) {
                        continue;
                    }
                    result = (result == null) ? step : compose(result, step);
                }
                return result;
            }
            if (array.size() == 4) {
                return quaternionMatrix(array.get(0).getAsDouble(), array.get(1).getAsDouble(),
                        array.get(2).getAsDouble(), array.get(3).getAsDouble());
            }
            if (array.size() == 3) {
                return eulerMatrix(readVec3(array, 3, tagName));
            }
            throw new JsonParseException(tagName
                    + ": expected a 3-float Euler array, a 4-float quaternion or an array of axis objects, got "
                    + array.size() + " element(s)");
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.entrySet().size() == 3 && obj.has("x") && obj.has("y") && obj.has("z")) {
                return eulerMatrix(new Vector3d(obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(), obj.get("z").getAsDouble()));
            }
            return parseAxisRotation(element, tagName);
        }

        throw new JsonParseException(tagName + ": expected an array or an object");
    }

    /**
     * Parses a single-axis rotation object ({@code {"x"|"y"|"z": degrees}}).
     */
    @Nullable
    private static Matrix4d parseAxisRotation(@Nullable JsonElement element, String tagName) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonObject()) {
            throw new JsonParseException(tagName + " axis rotation: object expected, got " + element);
        }
        JsonObject obj = element.getAsJsonObject();
        if (obj.entrySet().size() != 1) {
            throw new JsonParseException(tagName + " axis rotation: expected a single axis key, got " + obj.entrySet());
        }
        Map.Entry<String, JsonElement> axis = obj.entrySet().iterator().next();
        double degrees = axis.getValue().getAsDouble();

        Matrix4d matrix = new Matrix4d();
        switch (axis.getKey()) {
            case "x":
                matrix.rotX(Math.toRadians(degrees));
                return matrix;
            case "y":
                matrix.rotY(Math.toRadians(degrees));
                return matrix;
            case "z":
                matrix.rotZ(Math.toRadians(degrees));
                return matrix;
            default:
                throw new JsonParseException(tagName + " axis rotation: expected x, y or z, got " + axis.getKey());
        }
    }

    /**
     * Euler rotation in degrees, applied as {@code Rx x Ry x Rz} (the JOML
     * {@code Quaternionf#rotationXYZ} order Forge uses for its 3-float form).
     */
    private static Matrix4d eulerMatrix(Vector3d degrees) {
        Matrix4d rx = new Matrix4d();
        rx.rotX(Math.toRadians(degrees.x));
        Matrix4d ry = new Matrix4d();
        ry.rotY(Math.toRadians(degrees.y));
        Matrix4d rz = new Matrix4d();
        rz.rotZ(Math.toRadians(degrees.z));
        return compose(compose(rx, ry), rz);
    }

    /**
     * Quaternion to rotation matrix without normalization (a non-unit quaternion
     * also scales the model), matching Forge's {@code TransformationHelper.makeQuaternion}.
     */
    private static Matrix4d quaternionMatrix(double x, double y, double z, double w) {
        Matrix4d matrix = new Matrix4d();
        matrix.m00 = w * w + x * x - y * y - z * z;
        matrix.m01 = 2.0 * (x * y - w * z);
        matrix.m02 = 2.0 * (x * z + w * y);
        matrix.m10 = 2.0 * (x * y + w * z);
        matrix.m11 = w * w - x * x + y * y - z * z;
        matrix.m12 = 2.0 * (y * z - w * x);
        matrix.m20 = 2.0 * (x * z - w * y);
        matrix.m21 = 2.0 * (y * z + w * x);
        matrix.m22 = w * w - x * x - y * y + z * z;
        matrix.m33 = 1.0;
        return matrix;
    }

    // ==================== Matrix / vector helpers ====================

    /**
     * Parses the 3x4 affine matrix form; the fourth row is fixed to (0,0,0,1).
     */
    private static Matrix4d parseMatrix(JsonArray rows) {
        if (rows.size() != 3) {
            throw new JsonParseException(
                    "transform matrix must have 3 rows (3x4 affine), got " + rows.size());
        }
        Matrix4d matrix = new Matrix4d();
        matrix.setIdentity();
        for (int row = 0; row < 3; row++) {
            JsonElement rowElement = rows.get(row);
            if (!rowElement.isJsonArray()) {
                throw new JsonParseException("transform matrix row " + row + " must be an array");
            }
            JsonArray cells = rowElement.getAsJsonArray();
            if (cells.size() != 4) {
                throw new JsonParseException(
                        "transform matrix row " + row + " must hold 4 values, got " + cells.size());
            }
            double m0 = cells.get(0).getAsDouble();
            double m1 = cells.get(1).getAsDouble();
            double m2 = cells.get(2).getAsDouble();
            double m3 = cells.get(3).getAsDouble();
            if (row == 0) {
                matrix.m00 = m0;
                matrix.m01 = m1;
                matrix.m02 = m2;
                matrix.m03 = m3;
            } else if (row == 1) {
                matrix.m10 = m0;
                matrix.m11 = m1;
                matrix.m12 = m2;
                matrix.m13 = m3;
            } else {
                matrix.m20 = m0;
                matrix.m21 = m1;
                matrix.m22 = m2;
                matrix.m23 = m3;
            }
        }
        return matrix;
    }

    /**
     * Reads the first three values of an array tag into a vector after checking
     * its length, for tags whose exact size is already known.
     */
    private static Vector3d readVec3(JsonArray array, int expected, String tagName) {
        if (array.size() != expected) {
            throw new JsonParseException(
                    tagName + " must contain exactly " + expected + " values, got " + array.size());
        }
        return new Vector3d(array.get(0).getAsDouble(), array.get(1).getAsDouble(),
                array.get(2).getAsDouble());
    }

    /**
     * Reads a 3-float array tag.
     *
     * @return the vector, or {@code null} when the tag is missing
     */
    @Nullable
    private static Vector3d readVec3(@Nullable JsonElement element, String tagName) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException(tagName + " must be an array of 3 numbers");
        }
        return readVec3(element.getAsJsonArray(), 3, tagName);
    }

    /**
     * Reads {@code scale}: a uniform number or a 3-float array.
     *
     * @return the scale vector, or {@code null} when the tag is missing
     */
    @Nullable
    private static Vector3d readScale(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonArray()) {
            return readVec3(element.getAsJsonArray(), 3, "scale");
        }
        if (element.isJsonPrimitive()) {
            double uniform = element.getAsDouble();
            return new Vector3d(uniform, uniform, uniform);
        }
        throw new JsonParseException("scale must be a number or an array of 3 numbers");
    }

    /**
     * Reads {@code origin}: a preset name ({@code center} / {@code corner} /
     * {@code opposing-corner}) or explicit block-space coordinates.
     *
     * @return the pivot; Forge's default {@code opposing-corner} when the tag is missing
     */
    private static Vector3d readOrigin(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return DEFAULT_ORIGIN;
        }
        if (element.isJsonArray()) {
            return readVec3(element.getAsJsonArray(), 3, "origin");
        }
        if (element.isJsonPrimitive()) {
            String name = element.getAsString();
            switch (name) {
                case "center":
                    return new Vector3d(0.5, 0.5, 0.5);
                case "corner":
                    return new Vector3d(0.0, 0.0, 0.0);
                case "opposing-corner":
                    return new Vector3d(1.0, 1.0, 1.0);
                default:
                    throw new JsonParseException(
                            "origin: expected 'center', 'corner', 'opposing-corner' or [x, y, z], got '" + name + "'");
            }
        }
        throw new JsonParseException("origin must be a preset name or an array of 3 numbers");
    }

    /**
     * Multiplies two matrices into a fresh instance; neither argument is mutated,
     * so callers can safely compose cached matrices.
     */
    private static Matrix4d compose(Matrix4d left, Matrix4d right) {
        Matrix4d result = new Matrix4d();
        result.mul(left, right);
        return result;
    }
}
