stonecutter active "1.21.1"

stonecutter parameters {
    replacements {
        // HolderLookup.Provider (used for NBT) appeared in 1.19.3; the closest older type is RegistryAccess.
        string(current.parsed < "1.19.3") {
            replace("import net.minecraft.core.HolderLookup;", "import net.minecraft.core.RegistryAccess;")
            replace("HolderLookup.Provider", "RegistryAccess")
        }
        // Player rendering classes were renamed to "Avatar" in 1.21.9.
        string(current.parsed >= "1.21.9") {
            replace("PlayerRenderState", "AvatarRenderState")
            replace("extends PlayerRenderer {", "extends AvatarRenderer<AbstractClientPlayer> {")
            replace("entity.player.PlayerRenderer;", "entity.player.AvatarRenderer;")
        }
        // Mojang renamed ResourceLocation to Identifier in 1.21.11.
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
            replace("dimension().location()", "dimension().identifier()")
            // Classes moved into sub-packages in 1.21.11.
            replace("import net.minecraft.Util;", "import net.minecraft.util.Util;")
            replace("entity.monster.Spider;", "entity.monster.spider.Spider;")
            replace("client.model.PlayerModel;", "client.model.player.PlayerModel;")
            replace("client.renderer.RenderType;", "client.renderer.rendertype.RenderType;")
            replace("RenderType.entity", "net.minecraft.client.renderer.rendertype.RenderTypes.entity")
            replace("RenderType::entity", "net.minecraft.client.renderer.rendertype.RenderTypes::entity")
            // Game rules were rewritten in 1.21.11.
            replace("world.level.GameRules;", "world.level.gamerules.GameRules;")
            replace(".getBoolean(GameRules.RULE_KEEPINVENTORY)", ".get(GameRules.KEEP_INVENTORY)")
            replace(".getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)", ".get(GameRules.SHOW_DEATH_MESSAGES)")
            replace(".getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)", ".get(GameRules.FORGIVE_DEAD_PLAYERS)")
        }
        // 26.1 renamed GUI drawing to "render state extraction" and moved render-state classes.
        string(current.parsed >= "26.1") {
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("graphics.drawString(", "graphics.text(")
            replace("graphics.submitEntityRenderState(", "graphics.entity(")
            replace("guiRenderState.submitGuiElement(", "guiRenderState.addGuiElement(")
            replace("gui.render.state.GuiElementRenderState", "renderer.state.gui.GuiElementRenderState")
            replace("renderer.state.CameraRenderState", "renderer.state.level.CameraRenderState")
            replace("model.AtlasManager", "model.sprite.AtlasManager")
        }
        // 26.2 moved screen handling into Minecraft#gui and entity type constants into EntityTypes.
        string(current.parsed >= "26.2") {
            replace("client.screen", "client.gui.screen()")
            replace("minecraft.screen", "minecraft.gui.screen()")
            replace("client.setScreen(", "client.gui.setScreen(")
            replace("minecraft.setScreen(", "minecraft.gui.setScreen(")
            replace("EntityType.PLAYER", "net.minecraft.world.entity.EntityTypes.PLAYER")
        }
    }
}
