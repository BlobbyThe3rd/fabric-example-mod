package com.example.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;

public class MixinMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerStructureCommands(dispatcher);
        });
    }

    private void registerStructureCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("structure")
            .then(CommandManager.literal("save")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                BlockPos from = BlockPosArgumentType.getBlockPos(ctx, "from");
                                BlockPos to = BlockPosArgumentType.getBlockPos(ctx, "to");
                                ServerCommandSource source = ctx.getSource();

                                BlockPos min = new BlockPos(
                                        Math.min(from.getX(), to.getX()),
                                        Math.min(from.getY(), to.getY()),
                                        Math.min(from.getZ(), to.getZ()));
                                BlockPos max = new BlockPos(
                                        Math.max(from.getX(), to.getX()),
                                        Math.max(from.getY(), to.getY()),
                                        Math.max(from.getZ(), to.getZ()));

                                BlockBox box = new BlockBox(min, max);

                                StructureTemplateManager manager = source.getServer().getStructureTemplateManager();
                                Structure template = new Structure();
                                template.saveFromWorld(source.getWorld(), min, box, true, null);

                                try {
                                    Identifier id = new Identifier("structurecommand", name);
                                    manager.setTemplate(id, template);
                                    manager.save(id);
                                    source.sendFeedback(() -> Text.literal("Saved structure as " + id), false);
                                } catch (IOException e) {
                                    source.sendError(Text.literal("Failed to save structure: " + e.getMessage()));
                                }

                                return 1;
                            })))))
            .then(CommandManager.literal("load")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                            ServerCommandSource source = ctx.getSource();

                            Identifier id = new Identifier("structurecommand", name);
                            StructureTemplateManager manager = source.getServer().getStructureTemplateManager();

                            Structure template = manager.getTemplate(id);
                            if (template == null) {
                                source.sendError(Text.literal("Structure not found: " + id));
                                return 0;
                            }

                            StructurePlacementData settings = new StructurePlacementData();
                            template.place(source.getWorld(), pos, pos, settings, source.getWorld().getRandom(), 2);

                            source.sendFeedback(() -> Text.literal("Loaded structure " + id), false);
                            return 1;
                        })))));
    }
}
