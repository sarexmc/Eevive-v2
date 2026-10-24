package com.revivalcampfire;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Logica del ritual en el servidor (tiempos, sonidos, figura del jugador y desbaneo). */
public class RitualManager {
    public static final int TOTAL = 420;          // 21 segundos
    private static final int FLASH_TICK = 240;    // destello blanco
    private static final int STAND_START = 255;   // aparece la figura
    private static final int STAND_RISE = 85;     // ticks que sube
    private static final int UNBAN_TICK = 340;    // se desbanea

    private static final List<Ritual> ACTIVE = new ArrayList<>();

    /** Perfiles baneados. Usa solo API publica (getUser() es package-private en 1.20.1). */
    private static List<GameProfile> bannedProfiles(MinecraftServer server) {
        UserBanList bans = server.getPlayerList().getBans();
        List<GameProfile> out = new ArrayList<>();
        for (String key : bans.getUserList()) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            UserBanListEntry entry = bans.get(new GameProfile(id, null));
            if (entry == null) continue;
            String name = entry.getDisplayName().getString();
            out.add(new GameProfile(id, name));
        }
        return out;
    }

    public static List<ReviveMenu.Entry> banned(MinecraftServer server) {
        List<ReviveMenu.Entry> out = new ArrayList<>();
        for (GameProfile p : bannedProfiles(server)) {
            out.add(new ReviveMenu.Entry(p.getId(), p.getName()));
        }
        out.sort(Comparator.comparing(x -> x.name().toLowerCase()));
        return out;
    }

    private static GameProfile findBanned(MinecraftServer server, UUID id) {
        for (GameProfile p : bannedProfiles(server)) {
            if (id.equals(p.getId())) return p;
        }
        return null;
    }

    /** En singleplayer siempre se puede usar; en servidor se exige el nivel de permiso de la config. */
    public static boolean canUse(ServerPlayer p) {
        return p.server.isSingleplayer() || p.hasPermissions(Config.REQUIRED_PERMISSION.get());
    }

    private static GameProfile fakeProfile(String name) {
        return new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)), name);
    }

    /** Ritual de demostracion con un jugador ficticio (no necesita estar baneado). */
    public static Component startDemo(ServerLevel level, Vec3 center, String name) {
        GameProfile profile = fakeProfile(name);
        for (Ritual r : ACTIVE) {
            if (r.profile.getId().equals(profile.getId())) {
                return Component.translatable("msg.revivalcampfire.in_progress", name);
            }
        }
        ACTIVE.add(new Ritual(level, center, profile));
        return null;
    }

    /** Banea a un jugador ficticio para poder probar el menu. */
    public static void addTestBan(MinecraftServer server, String name) {
        UserBanList bans = server.getPlayerList().getBans();
        bans.add(new UserBanListEntry(fakeProfile(name), new Date(), "RevivalCampfire", null, "Baneo de prueba"));
        try {
            bans.save();
        } catch (IOException ignored) {
        }
    }

    /** @return null si todo va bien, o el mensaje de error. */
    public static Component start(ServerLevel level, Vec3 center, UUID id) {
        GameProfile profile = findBanned(level.getServer(), id);
        if (profile == null) return Component.translatable("msg.revivalcampfire.not_banned", id.toString());
        for (Ritual r : ACTIVE) {
            if (r.profile.getId().equals(id)) {
                return Component.translatable("msg.revivalcampfire.in_progress", profile.getName());
            }
        }
        ACTIVE.add(new Ritual(level, center, profile));
        return null;
    }

    public static void tick() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || ACTIVE.isEmpty()) return;
        Iterator<Ritual> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Ritual r = it.next();
            r.age++;
            if (!r.step(server)) {
                r.cleanup();
                it.remove();
            }
        }
    }

    public static void shutdown() {
        for (Ritual r : ACTIVE) r.cleanup();
        ACTIVE.clear();
    }

    private static class Ritual {
        final ServerLevel level;
        final Vec3 center;
        final GameProfile profile;
        int age = 0;
        ArmorStand stand;

        Ritual(ServerLevel level, Vec3 center, GameProfile profile) {
            this.level = level;
            this.center = center;
            this.profile = profile;
        }

        boolean step(MinecraftServer server) {
            double x = center.x, y = center.y, z = center.z;

            if (age == 1) {
                Network.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                                new PacketDistributor.TargetPoint(x, y, z, 160, level.dimension())),
                        new Network.StartRitual(x, y, z));
                level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 3f, 0.7f);
            }
            if (age == 100) {
                level.playSound(null, x, y, z, SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT, 3f, 0.6f);
            }
            if (age % 20 == 1 && age < 320) {
                Component msg = Component.translatable("msg.revivalcampfire.prepared", profile.getName())
                        .withStyle(ChatFormatting.AQUA);
                for (ServerPlayer p : level.players()) {
                    if (p.distanceToSqr(center) < 96 * 96) p.displayClientMessage(msg, true);
                }
            }
            if (age == FLASH_TICK) {
                level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 6f, 0.8f);
            }
            if (age == STAND_START) spawnStand();
            if (stand != null && stand.isAlive()) {
                double rise = Math.min(age - STAND_START, STAND_RISE) * 0.05;
                stand.setPos(x, y + rise, z);
                stand.setYRot(age * 7f);
            }
            if (age == UNBAN_TICK) {
                UserBanList bans = server.getPlayerList().getBans();
                bans.remove(profile);
                try {
                    bans.save();
                } catch (IOException ignored) {
                }
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("msg.revivalcampfire.revived", profile.getName())
                                .withStyle(ChatFormatting.GREEN), false);
                level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.AMBIENT, 4f, 1f);
            }
            return age < TOTAL;
        }

        void spawnStand() {
            ArmorStand st = new ArmorStand(level, center.x, center.y, center.z);
            st.setNoGravity(true);
            st.setInvulnerable(true);
            st.setInvisible(true);
            st.setNoBasePlate(true);
            st.setGlowingTag(true);

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.getOrCreateTag().put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), profile));
            st.setItemSlot(EquipmentSlot.HEAD, head);
            st.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE));
            st.setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS));
            st.setItemSlot(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS));
            level.addFreshEntity(st);
            this.stand = st;
        }

        private static ItemStack dyed(net.minecraft.world.item.Item item) {
            ItemStack s = new ItemStack(item);
            ((DyeableLeatherItem) item).setColor(s, 0x0B3B33);
            return s;
        }

        void cleanup() {
            if (stand != null) stand.discard();
        }
    }
}
