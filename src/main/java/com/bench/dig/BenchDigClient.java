package com.bench.dig;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.ArrayList;
import java.util.List;

public class BenchDigClient implements ClientModInitializer {
    private static final List<BlockPos> blocksToDig = new ArrayList<>();
    private static int tickDelay = 0;
    private static final int DELAY_TICKS = 4; 
    private static BlockPos currentTarget = null;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("kop")
                .executes(context -> {
                    String[] args = context.getInput().split(" ");
                    if (args.length == 5) {
                        try {
                            int x1 = Integer.parseInt(args[1]);
                            int z1 = Integer.parseInt(args[2]);
                            int x2 = Integer.parseInt(args[3]);
                            int z2 = Integer.parseInt(args[4]);
                            startDigging(x1, z1, x2, z2);
                        } catch (NumberFormatException e) {
                            context.getSource().sendError(net.minecraft.text.Text.literal("Zle kordy."));
                        }
                    } else {
                        context.getSource().sendError(net.minecraft.text.Text.literal("Uzyj: /kop x1 z1 x2 z2"));
                    }
                    return 1;
                })
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            if (currentTarget == null && !blocksToDig.isEmpty()) {
                currentTarget = blocksToDig.remove(0);
                sendAction(client, currentTarget, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK);
                tickDelay = DELAY_TICKS;
            } else if (currentTarget != null) {
                tickDelay--;
                if (tickDelay <= 0) {
                    sendAction(client, currentTarget, PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK);
                    currentTarget = null;
                }
            }
        });
    }

    private static void sendAction(MinecraftClient client, BlockPos pos, PlayerActionC2SPacket.Action action) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(action, pos, Direction.UP, 0));
        }
    }

    private static void startDigging(int x1, int z1, int x2, int z2) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        blocksToDig.clear();
        currentTarget = null;
        
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int startY = (int) client.player.getY();

        for (int y = startY + 4; y >= startY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocksToDig.add(new BlockPos(x, y, z));
                }
            }
        }
        client.player.sendMessage(net.minecraft.text.Text.literal("§aRozpoczeto kopanie " + blocksToDig.size() + " blokow."), false);
    }
}
