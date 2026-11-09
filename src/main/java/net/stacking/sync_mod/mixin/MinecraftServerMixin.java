package net.stacking.sync_mod.mixin;

import net.stacking.sync_mod.util.nbt.Nbt;
import net.stacking.sync_mod.api.shell.Shell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.util.nbt.OfflinePlayerNbtManager;
import net.stacking.sync_mod.api.shell.ShellStateManager;
import net.stacking.sync_mod.api.shell.ShellStateUpdateType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.stacking.sync_mod.util.Tuple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin implements ShellStateManager {
    @Shadow
    private PlayerList playerList;

    @Unique
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Tuple<ShellStateUpdateType, ShellState>>> pendingShellStates = new ConcurrentHashMap<>();

    @Override
    public void setAvailableShellStates(UUID owner, Stream<ShellState> states) {
        Shell shell = this.getShellById(owner);
        if (shell != null) {
            shell.setAvailableShellStates(states);
        }
    }

    @Override
    public Stream<ShellState> getAvailableShellStates(UUID owner) {
        Shell shell = this.getShellById(owner);
        return shell == null ? Stream.of() : shell.getAvailableShellStates();
    }

    @Override
    public ShellState getShellStateByUuid(UUID owner, UUID uuid) {
        Shell shell = this.getShellById(owner);
        return shell == null ? null : shell.getShellStateByUuid(uuid);
    }

    @Override
    public void add(ShellState state) {
        Shell shell = this.getShellByItsState(state);
        if (shell != null) {
            shell.add(state);
        }
    }

    @Override
    public void remove(ShellState state) {
        Shell shell = this.getShellByItsState(state);
        if (shell == null) {
            this.putPendingUpdate(state, ShellStateUpdateType.REMOVE);
        } else {
            shell.remove(state);
        }
    }

    @Override
    public void update(ShellState state) {
        Shell shell = this.getShellByItsState(state);
        if (shell == null) {
            this.putPendingUpdate(state, ShellStateUpdateType.UPDATE);
        } else {
            shell.update(state);
        }
    }

    @Override
    public Collection<Tuple<ShellStateUpdateType, ShellState>> peekPendingUpdates(UUID owner) {
        Map<UUID, Tuple<ShellStateUpdateType, ShellState>> shells = this.pendingShellStates.get(owner);
        if (shells == null) {
            return List.of();
        }
        return shells.values();
    }

    @Override
    public void clearPendingUpdates(UUID owner) {
        this.pendingShellStates.remove(owner);
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;removeAll()V"))
    private void onShutdown(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer)(Object)this;

        for (Map.Entry<UUID, ConcurrentMap<UUID, Tuple<ShellStateUpdateType, ShellState>>> entry : this.pendingShellStates.entrySet()) {
            UUID userId = entry.getKey();
            Collection<Tuple<ShellStateUpdateType, ShellState>> updates = entry.getValue().values();
            if (updates.isEmpty()) {
                continue;
            }

            // Use the corrected OfflinePlayerNbtManager.editPlayerNbt signature
            OfflinePlayerNbtManager.getPlayerNbt(server, userId).ifPresent(nbt -> {
                ListTag shellList = Nbt.contains(nbt, "Shells", Tag.TAG_LIST)
                        ? Nbt.getList(nbt, "Shells", Tag.TAG_COMPOUND)
                        : new ListTag();

                Map<UUID, ShellState> shellsMap = new HashMap<>();
                for (Tag element : shellList) {
                    if (element instanceof CompoundTag shellNbt) {
                        ShellState state = ShellState.fromNbt(shellNbt);
                        shellsMap.put(state.getUuid(), state);
                    }
                }

                for (Tuple<ShellStateUpdateType, ShellState> update : updates) {
                    ShellState state = update.getB();
                    switch (update.getA()) {
                        case ADD, UPDATE -> shellsMap.put(state.getUuid(), state);
                        case REMOVE -> shellsMap.remove(state.getUuid());
                    }
                }

                ListTag newShellList = new ListTag();
                shellsMap.values().stream()
                        .map(s -> s.writeNbt(new CompoundTag()))
                        .forEach(newShellList::add);

                nbt.put("Shells", newShellList);

                // Save the modified NBT back
                OfflinePlayerNbtManager.savePlayerNbt(server, userId, nbt);
            });
        }
    }

    @Unique
    private void putPendingUpdate(ShellState state, ShellStateUpdateType type) {
        this.pendingShellStates
                .computeIfAbsent(state.getOwnerUuid(), x -> new ConcurrentHashMap<>())
                .put(state.getUuid(), new Tuple<>(type, state));
    }

    @Unique
    private Shell getShellById(UUID id) {
        return id == null ? null : (Shell)this.playerList.getPlayer(id);
    }

    @Unique
    private Shell getShellByItsState(ShellState state) {
        return state == null ? null : this.getShellById(state.getOwnerUuid());
    }
}