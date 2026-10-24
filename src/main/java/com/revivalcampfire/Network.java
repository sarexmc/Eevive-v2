package com.revivalcampfire;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

public class Network {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RevivalCampfireMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, ReviveRequest.class, ReviveRequest::encode, ReviveRequest::decode, ReviveRequest::handle);
        CHANNEL.registerMessage(id++, StartRitual.class, StartRitual::encode, StartRitual::decode, StartRitual::handle);
    }

    /** Cliente -> Servidor: el jugador pulso "Revivir" en el menu. */
    public static class ReviveRequest {
        private final BlockPos pos;
        private final UUID target;

        public ReviveRequest(BlockPos pos, UUID target) {
            this.pos = pos;
            this.target = target;
        }

        public static void encode(ReviveRequest m, FriendlyByteBuf buf) {
            buf.writeBlockPos(m.pos);
            buf.writeUUID(m.target);
        }

        public static ReviveRequest decode(FriendlyByteBuf buf) {
            return new ReviveRequest(buf.readBlockPos(), buf.readUUID());
        }

        public static void handle(ReviveRequest m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) return;
                if (!(p.containerMenu instanceof ReviveMenu menu) || !menu.pos.equals(m.pos)) return;
                if (!RitualManager.canUse(p)) return;
                Component err = RitualManager.start(p.serverLevel(),
                        new Vec3(m.pos.getX() + 0.5, m.pos.getY() + 0.5, m.pos.getZ() + 0.5), m.target);
                p.closeContainer();
                if (err != null) p.displayClientMessage(err, true);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Servidor -> Clientes cercanos: arranca el ritual visual. */
    public static class StartRitual {
        private final double x, y, z;

        public StartRitual(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static void encode(StartRitual m, FriendlyByteBuf buf) {
            buf.writeDouble(m.x);
            buf.writeDouble(m.y);
            buf.writeDouble(m.z);
        }

        public static StartRitual decode(FriendlyByteBuf buf) {
            return new StartRitual(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        public static void handle(StartRitual m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientRitual.start(new Vec3(m.x, m.y, m.z))));
            ctx.get().setPacketHandled(true);
        }
    }
}
