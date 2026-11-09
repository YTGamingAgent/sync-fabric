package net.stacking.sync_mod.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.stacking.sync_mod.api.shell.Shell;
import net.stacking.sync_mod.api.shell.ShellState;
import net.stacking.sync_mod.api.shell.ShellStateContainer;
import net.stacking.sync_mod.util.WorldUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class GhostShellsCommand implements Command {
    private static final SimpleCommandExceptionType INVALID_ACTION_TYPE = new SimpleCommandExceptionType(Component.translatable("command.sync.ghostshells.invalid_action"));

    @Override
    public String getName() {
        return "ghostshells";
    }

    @Override
    public boolean hasPermissions(CommandSourceStack commandSource) {
        final int OP_LEVEL = 2;
        //? if >=1.21.11 {
        /*boolean isOp = commandSource.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else
        boolean isOp = commandSource.hasPermission(OP_LEVEL);
        // The server can be missing: since 26.2 permission checks are also run on a placeholder source.
        MinecraftServer server = commandSource.getServer();
        return isOp || (server != null && server.isSingleplayer());
    }

    @Override
    public void build(ArgumentBuilder<CommandSourceStack, ?> builder) {
        builder.then(Commands.argument("type", StringArgumentType.word())
            .suggests((a, b) -> SharedSuggestionProvider.suggest(Set.of("sync", "remove", "repair"), b))
            .then(Commands.argument("target", EntityArgument.players())
                .executes(GhostShellsCommand::execute)
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(GhostShellsCommand::execute)
                )
            )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String type = StringArgumentType.getString(context, "type");
        boolean repair;
        boolean canSkip;

        switch (type) {
            case "sync" -> {
                repair = true;
                canSkip = true;
            }
            case "remove" -> {
                repair = false;
                canSkip = true;
            }
            case "repair" -> {
                repair = true;
                canSkip = false;
            }
            default -> throw INVALID_ACTION_TYPE.create();
        }

        //? if >=1.20 {
        Consumer<Component> logger = x -> context.getSource().sendSuccess(() -> x, false);
        //?} else
        /*Consumer<Component> logger = x -> context.getSource().sendSuccess(x, false);*/
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "target");
        BlockPos pos;
        try {
            //? if >=1.19.4 {
            pos = BlockPosArgument.getBlockPos(context, "pos");
            //?} else
            /*pos = BlockPosArgument.getLoadedBlockPos(context, "pos");*/
        } catch (IllegalArgumentException e) {
            pos = null;
        }

        if (pos == null) {
            for (ServerPlayer player : players) {
                updateShells(player, repair, canSkip, logger);
            }
        } else {
            BlockPos finalPos = pos;
            for (ServerPlayer player : players) {
                ShellState shellState = ((Shell)player).getAvailableShellStates().filter(x -> x.getPos().equals(finalPos)).findAny().orElse(null);
                if (shellState == null) {
                    logger.accept(Component.translatable("command.sync.ghostshells.not_found", player.getName().getString(), pos.toShortString()));
                } else {
                    updateShell(player, shellState, repair, canSkip, logger);
                }
            }
        }
        return 1;
    }

    private static void updateShells(ServerPlayer player, boolean shouldRepair, boolean skipOnFailure, Consumer<Component> logger) {
        for (ShellState shellState : (Iterable<ShellState>)((Shell)player).getAvailableShellStates()::iterator) {
            shellState.setProgress(100);
            updateShell(player, shellState, shouldRepair, skipOnFailure, logger);
        }
    }

    private static void updateShell(ServerPlayer player, ShellState shellState, boolean shouldRepair, boolean skipOnFailure, Consumer<Component> logger) {
        if (shellExists(WorldUtil.server(player), shellState)) {
            return;
        }

        if (shouldRepair) {
            if (tryRepair(WorldUtil.server(player), shellState)) {
                logger.accept(Component.translatable("command.sync.ghostshells.repaired", player.getName().getString(), shellState.getPos().toShortString()));
                return;
            }

            if (!skipOnFailure) {
                logger.accept(Component.translatable("command.sync.ghostshells.failed", player.getName().getString(), shellState.getPos().toShortString()));
                return;
            }
        }

        ((Shell)player).remove(shellState);
        logger.accept(Component.translatable("command.sync.ghostshells.removed", player.getName().getString(), shellState.getPos().toShortString()));
    }

    private static boolean shellExists(MinecraftServer server, ShellState shellState) {
        return getShellContainer(server, shellState).map(x -> shellState.equals(x.getShellState())).orElse(Boolean.FALSE);
    }

    private static boolean tryRepair(MinecraftServer server, ShellState shellState) {
        ShellStateContainer shellContainer = getShellContainer(server, shellState).orElse(null);
        if (shellContainer == null) {
            return false;
        }

        if (shellContainer.getShellState() == null) {
            shellContainer.setShellState(shellState);
        }

        return shellState.equals(shellContainer.getShellState());
    }

    private static Optional<ShellStateContainer> getShellContainer(MinecraftServer server, ShellState shellState) {
        ServerLevel world = WorldUtil.findWorld(server.getAllLevels(), shellState.getWorld()).orElse(null);
        if (world == null) {
            return Optional.empty();
        }

        ChunkAccess chunk = world.getChunk(shellState.getPos());
        if (chunk == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(ShellStateContainer.find(world, shellState));
    }
}