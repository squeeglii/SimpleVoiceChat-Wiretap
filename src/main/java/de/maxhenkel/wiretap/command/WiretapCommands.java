package de.maxhenkel.wiretap.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.utils.HeadUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class WiretapCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("wiretap")
                .requires((commandSource) -> commandSource.hasPermission(Wiretap.SERVER_CONFIG.commandPermissionLevel.get()));

        literalBuilder.then(
                Commands.literal("items")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(WiretapCommands::runWithoutRange)
                        .then(
                                Commands.argument("speaker_radius", FloatArgumentType.floatArg(0.0f))
                                        .executes(WiretapCommands::runWithRange)
                        )
        );


        dispatcher.register(literalBuilder);
    }


    public static int runWithoutRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return WiretapCommands.processCommand(player, -1f);
    }

    public static int runWithRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        float range = FloatArgumentType.getFloat(context, "speaker_radius");

        return WiretapCommands.processCommand(player, range);
    }


    public static int processCommand(ServerPlayer player, float range) {
        try {
            UUID id = UUID.randomUUID();
            Optional<ItemStack> microphone = HeadUtils.createMicrophone(id);
            Optional<ItemStack> speaker = HeadUtils.createSpeakerWithForcedRange(id, range);

            if (microphone.isPresent() && speaker.isPresent()) {
                player.getInventory().add(microphone.get());
                player.getInventory().add(speaker.get());
                player.displayClientMessage(Component.literal("You have been provided a wiretap kit.").withStyle(ChatFormatting.RED), false);
                return 1;
            } else {
                player.displayClientMessage(Component.literal("There was an error while spawning in your wiretap kit.").withStyle(ChatFormatting.RED), false);
                return 0;
            }
        } catch (Exception e) {
            player.displayClientMessage(Component.literal("There was an unexpected error while spawning in your wiretap kit.").withStyle(ChatFormatting.RED), false);
            Wiretap.LOGGER.error("Error caught while executing command", e);
            return 0;
        }
    }
}
