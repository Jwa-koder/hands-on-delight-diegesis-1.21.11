package com.pockyl.diegesis.impl.client.scene;

import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.pockyl.diegesis.api.scene.PlacingOptions;
import com.pockyl.diegesis.api.scene.PlaceSurface;
import com.pockyl.diegesis.api.state.ItemPose;

import java.util.List;

/** Where items placed freely can stand: the geometry of the surfaces of a scene's {@code "placing"}. */
public final class Placing {
    /** How far (pixels) past its edges a surface still catches the cursor. */
    private static final float AIM_MARGIN = 1.5F;
    /** Hit this high on an item (0 its foot, 1 its top), the item in the hand goes on top of it instead of in front. */
    private static final float ON_TOP_FROM = 0.5F;
    /** An item this low (pixels) is all top: a book lying down. */
    private static final float LOW_ITEM = 2.5F;

    private Placing() {
    }

    /** A point on a surface the cursor is aimed at. */
    public record Aim(PlaceSurface surface, float x, float z) {
    }

    /** The surface the cursor ray finds first, or null. */
    public static Aim aim(PlacingOptions options, Vector3fc origin, Vector3fc direction) {
        return aim(options, origin, direction, null);
    }

    /**
     * The surface the cursor ray finds first, or null; surfaces the scene moves (on a door swung open) are found where they are
     * now, and the point is given on the surface as written.
     */
    public static Aim aim(PlacingOptions options, Vector3fc rayOrigin, Vector3fc rayDirection, SceneInstance scene) {
        Aim best = null;
        float nearest = Float.MAX_VALUE;
        for (int index = 0; index < options.surfaces().size(); index++) {
            PlaceSurface surface = options.surfaces().get(index);
            if (scene != null && !scene.isSurfaceVisible(index)) {
                // Shut away: it takes nothing.
                continue;
            }
            float[] hit = hit(surface, index, rayOrigin, rayDirection, scene);
            if (hit == null || hit[0] >= nearest || hit[1] < surface.from().x() - AIM_MARGIN || hit[1] > surface.to().x() + AIM_MARGIN
                    || hit[2] < surface.from().y() - AIM_MARGIN || hit[2] > surface.to().y() + AIM_MARGIN) {
                continue;
            }
            nearest = hit[0];
            best = new Aim(surface, hit[1], hit[2]);
        }
        return best;
    }

    /**
     * Where the cursor ray meets the plane of one surface, wherever that is, also past its edges: a thing slid along a shelf stays
     * on that shelf when the cursor leaves it, rather than jumping to whatever surface lies behind. Null when the ray misses the
     * plane.
     */
    public static Aim aimOn(PlacingOptions options, PlaceSurface surface, Vector3fc rayOrigin, Vector3fc rayDirection, SceneInstance scene) {
        float[] hit = hit(surface, options.surfaces().indexOf(surface), rayOrigin, rayDirection, scene);
        return hit == null ? null : new Aim(surface, hit[1], hit[2]);
    }

    /** The ray's length to the surface's plane, and x, z where it meets it (as the surface is written), or null. */
    private static float[] hit(PlaceSurface surface, int index, Vector3fc rayOrigin, Vector3fc rayDirection, SceneInstance scene) {
        org.joml.Matrix4f move = scene == null || index < 0 ? null : scene.surfaceMove(index);
        Vector3fc origin = rayOrigin;
        Vector3fc direction = rayDirection;
        if (move != null) {
            // The ray as the surface sees it: its move undone (turns and shifts keep the ray's length, so t still compares).
            org.joml.Matrix4f back = new org.joml.Matrix4f(move).invert();
            origin = back.transformPosition(new Vector3f(rayOrigin));
            direction = back.transformDirection(new Vector3f(rayDirection));
        }
        float depth = Math.max(0.01F, surface.to().y() - surface.from().y());
        float slope = surface.aimHeight() / depth;
        // The slope: y = height + slope * (z - front).
        float denominator = direction.y() - slope * direction.z();
        if (Math.abs(denominator) < 1.0e-6F) {
            return null;
        }
        float t = (surface.height() + slope * (origin.z() - surface.from().y()) - origin.y()) / denominator;
        return t <= 0.0F ? null : new float[] {t, origin.x() + direction.x() * t, origin.z() + direction.z() * t};
    }

    /** Whether a point hit on {@code below} means on top of it. */
    public static boolean isOnTop(ItemPose below, float hitY) {
        return below.lift() * 2.0F <= LOW_ITEM || hitY >= below.y() + below.lift() * 2.0F * ON_TOP_FROM;
    }

    /**
     * The surface an item standing at this pose belongs to, or null. Where the rooms of two surfaces overlap (a door's bin written
     * inside a shelf's room), the one whose floor is highest under the item: the one it stands on.
     */
    public static PlaceSurface surfaceOf(PlacingOptions options, ItemPose pose) {
        PlaceSurface found = null;
        for (PlaceSurface surface : options.surfaces()) {
            if (surface.holds(pose.x(), pose.y(), pose.z()) && (found == null || surface.height() > found.height())) {
                found = surface;
            }
        }
        return found;
    }

    /**
     * The surface of an item standing at {@code pose}: for one standing on another ({@code on}), that one's, down to the bottom
     * of the stack ({@code support} gives the pose of a slot). By its height alone, a thing on another near the front of a shelf
     * belonged to a door's bin written there, over the shelf's floor.
     */
    public static PlaceSurface surfaceOf(PlacingOptions options, ItemPose pose, java.util.function.IntFunction<ItemPose> support) {
        ItemPose at = pose;
        for (int depth = 0; at.on() >= 0 && depth < 32; depth++) {
            ItemPose under = support.apply(at.on());
            if (under == null) {
                break;
            }
            at = under;
        }
        return surfaceOf(options, at);
    }

    /** {@code item_size * scale} of an item, cut down so that it fits under what is above it. */
    public static float fittingScale(PlacingOptions options, PlacedShape shape, Quaternionfc rotation, float scale, float room) {
        float height = shape.extents(rotation, options.itemSize() * scale).y * 2.0F;
        return height <= room ? scale : scale * room / height;
    }

    /** An item standing on {@code surface} at {@code x, z}, kept within its edges; at {@code y} over what it stands on. */
    public static ItemPose standing(PlacingOptions options, PlacedShape shape, PlaceSurface surface, float x, float y, float z, Quaternionfc rotation,
            float wantedScale, int on) {
        float room = surface.height() + surface.clearance() - y;
        float scale = fittingScale(options, shape, rotation, wantedScale, room);
        Vector3f extents = shape.extents(rotation, options.itemSize() * scale);
        float keptX = keepWithin(snapped(options, x), surface.from().x(), surface.to().x(), extents.x);
        float keptZ = keepWithin(snapped(options, z), surface.from().y(), surface.to().y(), extents.z);
        return new ItemPose(keptX, y, keptZ, extents.y, rotation, scale, on);
    }

    private static float keepWithin(float value, float low, float high, float half) {
        return low + half > high - half ? (low + high) / 2.0F : Mth.clamp(value, low + half, high - half);
    }

    private static float snapped(PlacingOptions options, float value) {
        return options.snap() <= 0.0F ? value : Math.round(value / options.snap()) * options.snap();
    }

    /** Pixels between an item set out by {@link #freeSpot} and its neighbours, and the step its search goes in. */
    private static final float SPOT_GAP = 0.25F;
    private static final float SPOT_STEP = 0.5F;
    /** How much smaller (of {@code scale_range}'s least) each try of {@link #freeSpot} makes an item that does not fit. */
    private static final float SPOT_SHRINK = 0.15F;

    /**
     * A free place for an item nobody put down by hand (shift-clicked in, brought by a hopper): the first spot on {@code surfaces}
     * where it stands clear of the boxes {@code taken} (each {min x, max x, min y, max y, min z, max z}), searched from the back of
     * each surface to the front, so the newest stands in front where it is seen. At its full size where it fits, else smaller,
     * down to the least of {@code scale_range}. Null when there is no room left: the station is full.
     *
     * @param least the least half width and depth the item takes (a flat icon's collision depth)
     */
    public static ItemPose freeSpot(PlacingOptions options, PlacedShape shape, List<PlaceSurface> surfaces, List<float[]> taken, float least) {
        Quaternionf upright = new Quaternionf();
        float smallest = Math.min(1.0F, options.scaleRange()[0]);
        for (float scale = 1.0F; scale >= smallest - 1.0e-4F; scale -= SPOT_SHRINK) {
            Vector3f extents = shape.extents(upright, options.itemSize() * scale);
            float halfX = Math.max(extents.x, least) + SPOT_GAP * 0.5F;
            float halfZ = Math.max(extents.z, least) + SPOT_GAP * 0.5F;
            for (PlaceSurface surface : surfaces) {
                if (extents.y * 2.0F > surface.clearance() + 1.0e-3F) {
                    continue;
                }
                float bottom = surface.height();
                float top = bottom + extents.y * 2.0F;
                for (float z = surface.to().y() - halfZ; z >= surface.from().y() + halfZ - 1.0e-3F; z -= SPOT_STEP) {
                    for (float x = surface.from().x() + halfX; x <= surface.to().x() - halfX + 1.0e-3F; x += SPOT_STEP) {
                        if (clear(taken, x - halfX, x + halfX, bottom, top, z - halfZ, z + halfZ)) {
                            return new ItemPose(x, bottom, z, extents.y, upright, scale, -1);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean clear(List<float[]> taken, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        for (float[] box : taken) {
            if (box[1] > minX && box[0] < maxX && box[3] > minY + 0.02F && box[2] < maxY - 0.02F && box[5] > minZ && box[4] < maxZ) {
                return false;
            }
        }
        return true;
    }

    /** The box {min x, max x, min y, max y, min z, max z} an item takes at {@code pose}, at least {@code least} wide and deep. */
    public static float[] boxOf(PlacingOptions options, PlacedShape shape, ItemPose pose, float least) {
        Vector3f extents = shape.extents(pose.rotation(), options.itemSize() * pose.scale());
        float halfX = Math.max(extents.x, least);
        float halfZ = Math.max(extents.z, least);
        float middle = pose.y() + pose.lift();
        return new float[] {pose.x() - halfX, pose.x() + halfX, middle - extents.y, middle + extents.y, pose.z() - halfZ, pose.z() + halfZ};
    }

    /**
     * A place for an item nobody put down by hand: in rows along the surfaces, by its slot's number; only on the surfaces that are
     * not shut away ({@code hidden}, per surface; any when all are).
     */
    static ItemPose fallback(PlacingOptions options, int index, PlacedShape shape, boolean[] hidden) {
        List<PlaceSurface> open = new java.util.ArrayList<>();
        for (int i = 0; i < options.surfaces().size(); i++) {
            if (i >= hidden.length || !hidden[i]) {
                open.add(options.surfaces().get(i));
            }
        }
        if (open.isEmpty()) {
            open = options.surfaces();
        }
        PlaceSurface surface = open.get(index % open.size());
        int place = index / open.size();
        float width = surface.to().x() - surface.from().x();
        int perRow = Math.max(1, (int) (width / options.itemSize()));
        float x = surface.from().x() + (place % perRow + 0.5F) * width / perRow;
        float z = (surface.from().y() + surface.to().y()) / 2.0F;
        return standing(options, shape, surface, x, surface.height(), z, new Quaternionf(), 1.0F, -1);
    }
}
