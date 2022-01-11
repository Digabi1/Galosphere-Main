package net.orcinus.cavesandtrenches.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;
import net.orcinus.cavesandtrenches.CavesAndTrenches;
import net.orcinus.cavesandtrenches.client.entity.model.QuarkArmorModel;
import net.orcinus.cavesandtrenches.events.ClientEvents;
import net.orcinus.cavesandtrenches.init.CTItems;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SterlingArmorItem extends ArmorItem {
    private static final SterlingArmorMaterial material = new SterlingArmorMaterial();
    private static final String HELMET_TEXTURE = CavesAndTrenches.MODID + ":textures/entity/sterling_helmet.png";
    private static final String LEGS_TEXTURE = CavesAndTrenches.MODID + ":textures/entity/sterling_armor_2.png";
    private static final String TEXTURE = CavesAndTrenches.MODID + ":textures/entity/sterling_armor.png";

    public SterlingArmorItem(EquipmentSlot slot, Properties properties) {
        super(material, slot, properties);
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot == EquipmentSlot.HEAD) {
            return HELMET_TEXTURE;
        } else if (slot == EquipmentSlot.LEGS) {
            return LEGS_TEXTURE;
        } else {
            return TEXTURE;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {

            @Override
            @SuppressWarnings("unchecked")
            public <A extends HumanoidModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, A _default) {
                return (A) new QuarkArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(ClientEvents.STERLING_HELMET), EquipmentSlot.HEAD);
            }

        });
    }

    private static class SterlingArmorMaterial implements ArmorMaterial {
        private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};

        @Override
        public int getDurabilityForSlot(EquipmentSlot slot) {
            return HEALTH_PER_SLOT[slot.getIndex()] * 12;
        }

        @Override
        public int getDefenseForSlot(EquipmentSlot slot) {
            int[] slots = new int[]{1, 4, 5, 1};
            return slots[slot.getIndex()];
        }

        @Override
        public int getEnchantmentValue() {
            return 9;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_CHAIN;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(CTItems.SILVER_INGOT.get());
        }

        @Override
        public String getName() {
            return "sterling";
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    }
}