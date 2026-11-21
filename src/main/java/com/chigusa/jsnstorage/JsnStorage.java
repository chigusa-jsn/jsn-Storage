package com.chigusa.jsnstorage;

import com.chigusa.jsnstorage.menu.StorageMenu;
import com.chigusa.jsnstorage.storage.BackpackItem;
import com.chigusa.jsnstorage.storage.BackpackUpgradeRecipe;
import com.chigusa.jsnstorage.storage.KitItem;
import com.chigusa.jsnstorage.storage.Kind;
import com.chigusa.jsnstorage.storage.StorageBlock;
import com.chigusa.jsnstorage.storage.StorageBlockEntity;
import com.chigusa.jsnstorage.storage.Tier;
import com.chigusa.jsnstorage.upgrade.UpgradeItem;
import com.chigusa.jsnstorage.upgrade.UpgradeType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** jsn-Storage：ランクつきのチェスト・シュルカー・リュックと、アップグレード */
@Mod(JsnStorage.MODID)
public final class JsnStorage {
    public static final String MODID = "jsn_storage";
    public static final Logger LOG = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final String[] RANKS = {"copper", "gold", "diamond"};
    public static final String[] RANK_NAMES = {"銅", "金", "ダイヤ"};

    public static final Map<Tier, DeferredBlock<StorageBlock>> CHESTS = new EnumMap<>(Tier.class), SHULKERS = new EnumMap<>(Tier.class);
    public static final Map<Tier, DeferredItem<BackpackItem>> BACKPACKS = new EnumMap<>(Tier.class);
    public static final Map<Tier, DeferredItem<KitItem>> KITS = new EnumMap<>(Tier.class);
    public static final List<DeferredItem<UpgradeItem>> UPGRADES = new ArrayList<>();

    static {
        for (Tier t : Tier.values()) {
            BlockBehaviour.Properties base = BlockBehaviour.Properties.of().strength(2.5F).sound(t == Tier.WOOD ? SoundType.WOOD : SoundType.METAL)
                    .mapColor(t == Tier.WOOD ? MapColor.WOOD : MapColor.METAL);
            if (t == Tier.NETHERITE) base = base.strength(4F, 1200F);
            final BlockBehaviour.Properties bp = base;
            DeferredBlock<StorageBlock> c = BLOCKS.register(t.id + "_chest", () -> new StorageBlock(bp, t, Kind.CHEST));
            DeferredBlock<StorageBlock> s = BLOCKS.register(t.id + "_shulker_box", () -> new StorageBlock(
                    BlockBehaviour.Properties.of().strength(2F).sound(SoundType.SHULKER_BOX).mapColor(MapColor.COLOR_PURPLE).noOcclusion()
                            .pushReaction(PushReaction.DESTROY), t, Kind.SHULKER));
            CHESTS.put(t, c);
            SHULKERS.put(t, s);
            Item.Properties ip = new Item.Properties();
            if (t == Tier.NETHERITE) ip = ip.fireResistant();
            final Item.Properties fip = ip;
            ITEMS.register(t.id + "_chest", () -> new BlockItem(c.get(), fip));
            ITEMS.register(t.id + "_shulker_box", () -> new BlockItem(s.get(), new Item.Properties().stacksTo(1).fireResistant()));
            BACKPACKS.put(t, ITEMS.register(t.id + "_backpack", () -> {
                Item.Properties bp2 = new Item.Properties().stacksTo(1).rarity(t.ordinal() >= 3 ? Rarity.RARE : t.ordinal() >= 1 ? Rarity.UNCOMMON : Rarity.COMMON);
                return new BackpackItem(t == Tier.NETHERITE ? bp2.fireResistant() : bp2, t);
            }));
            if (t.next() != null) KITS.put(t, ITEMS.register(t.id + "_to_" + t.next().id + "_kit", () -> new KitItem(new Item.Properties(), t)));
        }
        for (UpgradeType u : UpgradeType.values()) {
            if (u.ranked) for (int r = 0; r < 3; r++) {
                int rr = r;
                UPGRADES.add(ITEMS.register(u.id + "_upgrade_" + RANKS[r], () -> new UpgradeItem(new Item.Properties().stacksTo(1), u, rr)));
            }
            else UPGRADES.add(ITEMS.register(u.id + "_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), u, 0)));
        }
    }

    public static final Supplier<BlockEntityType<StorageBlockEntity>> STORAGE_BE = BLOCK_ENTITIES.register("storage", () -> {
        List<Block> all = new ArrayList<>();
        CHESTS.values().forEach(b -> all.add(b.get()));
        SHULKERS.values().forEach(b -> all.add(b.get()));
        return BlockEntityType.Builder.of(StorageBlockEntity::new, all.toArray(new Block[0])).build(null);
    });
    public static final Supplier<MenuType<StorageMenu>> STORAGE_MENU = MENUS.register("storage", () -> IMenuTypeExtension.create(StorageMenu::client));
    public static final Supplier<RecipeSerializer<BackpackUpgradeRecipe>> BACKPACK_UPGRADE = SERIALIZERS.register("backpack_upgrade",
            () -> new SimpleCraftingRecipeSerializer<>(BackpackUpgradeRecipe::new));

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.literal("jsn-Storage")).icon(() -> new ItemStack(BACKPACKS.get(Tier.GOLD).get()))
            .displayItems((p, out) -> {
                for (Tier t : Tier.values()) out.accept(CHESTS.get(t).get());
                for (Tier t : Tier.values()) out.accept(SHULKERS.get(t).get());
                for (Tier t : Tier.values()) out.accept(BACKPACKS.get(t).get());
                KITS.values().forEach(k -> out.accept(k.get()));
                UPGRADES.forEach(u -> out.accept(u.get()));
            }).build());

    public static Block block(Kind k, Tier t) {
        return (k == Kind.SHULKER ? SHULKERS : CHESTS).get(t).get();
    }

    public static Item backpack(Tier t) {
        return BACKPACKS.get(t).get();
    }

    public JsnStorage(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        SERIALIZERS.register(bus);
        TABS.register(bus);
        bus.addListener(RegisterPayloadHandlersEvent.class, Net::register);
        bus.addListener(RegisterCapabilitiesEvent.class, e ->
                // ホッパーなどからも出し入れできるように
                e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STORAGE_BE.get(), (be, side) -> be.inv()));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
