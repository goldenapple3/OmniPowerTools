package goldenapple.omnitools.item;

import goldenapple.omnitools.config.RFToolProperties;
import goldenapple.omnitools.config.ToolProperties;
import goldenapple.omnitools.util.ToolHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;

@SuppressWarnings("NullableProblems")
public class ItemChainsawRF extends ItemChainsaw implements IRFContainerItem {
    protected RFToolProperties propertiesRF;

    public ItemChainsawRF(ToolProperties properties, RFToolProperties propertiesRF, float speed) {
        super(properties, speed);
        this.propertiesRF = propertiesRF;
        setHasSubtypes(true);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        if(state.getBlockHardness(world, pos) != 0 || (canShear(stack) && state.getBlock() instanceof IShearable))
            drainEnergy(stack, getEnergyUsage(stack, state), entity);
        return true;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        drainEnergy(stack, getEnergyUsage(stack) * 2, attacker);
        return true;
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity, EnumHand hand) {
        if(canShear(stack) && ToolHelper.shearEntity(stack, player, entity)){
            drainEnergy(stack, getEnergyUsage(stack), entity);
            return true;
        }
        return false;
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(item));
        list.add(setEnergy(new ItemStack(item), propertiesRF.maxEnergy));
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0d - (double) getEnergyStored(stack) / (double) getMaxEnergyStored(stack);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        tooltip.add(Integer.toString(getEnergyStored(stack)));
    }

    @Override
    public boolean isItemTool(ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxDamage() {
        return 0;
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        //This is done to prevent mining progress from resetting while the tool charges
        //I could compare NBT tags instead but I'm lazy
        return oldStack.getItem() == newStack.getItem();
    }

    public int getEnergyUsage(ItemStack stack){
        //Vanilla formula: a 100% / (unbreaking level + 1) chance to not take damage
        return Math.round(propertiesRF.energyPerBlock / (EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack) + 1));
    }

    public int getEnergyUsage(ItemStack stack, IBlockState state){
        if(canShear(stack) && state.getBlock() instanceof IShearable)
            return getEnergyUsage(stack) / 5;
        else
            return getEnergyUsage(stack);
    }

    @Override
    public boolean canMine(ItemStack stack) {
        return getEnergyStored(stack) > 0;
    }

    /** IRFTool */

    @Override
    public ItemStack setEnergy(ItemStack stack, int energy){
        stack.setTagInfo("Energy", new NBTTagInt(Math.min(energy, getMaxEnergyStored(stack))));
        return stack;
    }

    @Override
    public ItemStack drainEnergy(ItemStack stack, int energy, EntityLivingBase entity){
        if(entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode)
            return stack;
        setEnergy(stack, Math.max(getEnergyStored(stack) - energy, 0));
        if(getEnergyStored(stack) == 0 && properties.canBreak) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                ForgeEventFactory.onPlayerDestroyItem(player, stack, entity.getActiveHand());
                player.addStat(StatList.getObjectBreakStats(this));
            }
            entity.renderBrokenItemStack(stack);
            --stack.stackSize;
            stack.setItemDamage(0);
        }
        return stack;
    }

    /** IEnergyContainerItem */

    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        int energy = getEnergyStored(stack);
        int energyReceived = Math.min(propertiesRF.maxEnergy - energy, Math.min(propertiesRF.rechargeRate, maxReceive));

        if (!simulate)
            setEnergy(stack, energy + energyReceived);
        return energyReceived;
    }

    @Override
    public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        if(!stack.hasTagCompound())
            return 0;
        return stack.getTagCompound().getInteger("Energy");
    }

    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return propertiesRF.maxEnergy;
    }

}
