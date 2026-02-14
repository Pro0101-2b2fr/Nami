/*
 * Originally taken from https://github.com/mioclient/ oyvey-ported since im lazy to write my own renderers
 * Please take a note that this code can be sublicensed by its owner
 */

package namidevelopment.kiriyaga.api.util.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import net.minecraft.gizmos.Gizmos;
import org.joml.*;
import net.minecraft.core.BlockPos;
import org.lwjgl.opengl.GL11;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import java.awt.*;

public class RenderUtil {
    public static final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    public static final Matrix4f MODEL_VIEW_MATRIX = new Matrix4f();
    public static final Matrix4f POSITION_MATRIX = new Matrix4f();
    public static Camera CAMERA = new Camera();

    public static void rect3d(PoseStack matrix, float x1, float y1, float x2, float y2, int color) {
        float i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float j = (float) (color & 255) / 255.0F;

        BufferBuilder bufferBuilder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(matrix.last().pose(), x1, y2, 0.0F).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix.last().pose(), x2, y2, 0.0F).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix.last().pose(), x2, y1, 0.0F).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix.last().pose(), x1, y1, 0.0F).setColor(g, h, j, f);

        Layers.getGlobalQuads().draw(bufferBuilder.buildOrThrow());
    }

    // Author: crosby.moe
    public static void fade(GuiGraphics context, int x1, int y1, int x2, int y2, int c1, int c2, int c3, int c4) {
        context.guiRenderState.submitGlyphToCurrentLayer(new namidevelopment.kiriyaga.api.util.render.RectangleRenderState(new org.joml.Matrix3x2f(context.pose()), x1, y1, x2, y2, c1, c2, c3, c4, context.scissorStack.peek()));
    }

    // 3d
    //   net.minecraft.client.render.debug.ChunkBorderDebugRenderer
    public static void drawBlockPosLines(Level world, BlockPos pos, BlockState state, Color color, boolean filled, boolean outlined, float outlineWidth) {
        VoxelShape shape = state.getShape(world, pos);

        int fillColor = ARGB.color(
                50,
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );

        int outlineColor = ARGB.color(
                90,
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );

        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            AABB box = new AABB(
                    pos.getX() + minX,
                    pos.getY() + minY,
                    pos.getZ() + minZ,
                    pos.getX() + maxX,
                    pos.getY() + maxY,
                    pos.getZ() + maxZ
            );

            if (filled) {
                Gizmos.cuboid(
                        box,
                        GizmoStyle.fill(fillColor)
                ).setAlwaysOnTop();
            }

            if (outlined) {
                Gizmos.cuboid(
                        box,
                        GizmoStyle.stroke(outlineColor, outlineWidth)
                ).setAlwaysOnTop();
            }
        });
    }

    public static void drawBoxLines(AABB box, Color color, boolean filled, boolean outlined, float outlineWidth) {
        int fillColor = ARGB.color(
                50,
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );

        int outlineColor = ARGB.color(
                90,
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );

        if (filled) {
            Gizmos.cuboid(
                    box,
                    GizmoStyle.fill(fillColor)
            ).setAlwaysOnTop();
        }

        if (outlined) {
            Gizmos.cuboid(
                    box,
                    GizmoStyle.stroke(outlineColor, outlineWidth)
            ).setAlwaysOnTop();
        }
    }

    public static void drawLine(Vec3 from, Vec3 to, Color color, float width) {
        int lineColor = ARGB.color(
                255,
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );

        Gizmos.line(from, to, lineColor, width)
                .setAlwaysOnTop();
    }

    private static AABB cameraTransform(AABB box) {
        Vec3 camera = MC.gameRenderer.getMainCamera().position();
        return new AABB(box.minX - camera.x(),
                box.minY - camera.y(),
                box.minZ - camera.z(),
                box.maxX - camera.x(),
                box.maxY - camera.y(),
                box.maxZ - camera.z());
    }

    private static Vec3 cameraTransform(Vec3 vec3d) {
        Vec3 camera = MC.gameRenderer.getMainCamera().position();
        return new Vec3(vec3d.x - camera.x(),
                vec3d.y - camera.y(),
                vec3d.z - camera.z());
    }

    public static Vec3 project(Vec3 vec3d) {
        vec3d = cameraTransform(vec3d);

        int displayHeight = MC.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        Vector4f transformedCoordinates = new Vector4f((float) vec3d.x, (float) vec3d.y, (float) vec3d.z, 1.f).mul(POSITION_MATRIX);
        Matrix4f matrixProj = new Matrix4f(PROJECTION_MATRIX);
        Matrix4f matrixModel = new Matrix4f(MODEL_VIEW_MATRIX);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        double scale = MC.getWindow().getGuiScale();

        return new Vec3(target.x / scale, (displayHeight - target.y) / scale, target.z);
    }

    public static boolean projectionVisible(Vec3 vec3d) {
        return vec3d.z > 0 && vec3d.z < 1;
    }
}