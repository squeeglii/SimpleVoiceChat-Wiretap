package de.maxhenkel.wiretap.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.utils.HeadUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class WiretapCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("wiretap")
                .requires((commandSource) -> commandSource.hasPermission(Wiretap.SERVER_CONFIG.commandPermissionLevel.get()));

        literalBuilder.then(
                Commands.literal("items")
                        .executes(WiretapCommands::runWithoutRange)
                        .then(
                                Commands.argument("range", FloatArgumentType.floatArg(0.0f))
                                        .executes(WiretapCommands::runWithRange)
                        )
        );


        dispatcher.register(literalBuilder);
    }


    public static int runWithoutRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Wiretap.LOGGER.info("WITHOUT");

        return WiretapCommands.processCommand(player, -1f);
    }

    public static int runWithRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Wiretap.LOGGER.info("WITH");

        ServerPlayer player = context.getSource().getPlayerOrException();
        float range = context.getArgument("range", Float.class);

        return WiretapCommands.processCommand(player, range);
    }


    public static int processCommand(ServerPlayer player, float range) {
        UUID id = UUID.randomUUID();
        ItemStack microphone = HeadUtils.createMicrophone(id);
        ItemStack speaker = HeadUtils.createSpeaker(id, range);
        player.getInventory().add(microphone);
        player.getInventory().add(speaker);

        return 1;
    }
}
