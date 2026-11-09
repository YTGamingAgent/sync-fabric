package net.stacking.sync_mod.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public interface Command {
    String getName();

    boolean hasPermissions(CommandSourceStack commandSource);

    void build(ArgumentBuilder<CommandSourceStack, ?> builder);
}