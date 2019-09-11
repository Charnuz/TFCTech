package tfctech.objects.tileentities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import net.dries007.tfc.ConfigTFC;
import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.api.capability.heat.CapabilityItemHeat;
import net.dries007.tfc.api.capability.heat.IItemHeat;
import net.dries007.tfc.api.capability.metal.IMetalItem;
import net.dries007.tfc.api.recipes.heat.HeatRecipe;
import net.dries007.tfc.objects.te.ITileFields;
import net.dries007.tfc.objects.te.TEInventory;
import tfctech.ModConfig;
import tfctech.TFCTech;
import tfctech.objects.storage.MachineEnergyContainer;

import static net.dries007.tfc.api.capability.heat.CapabilityItemHeat.MAX_TEMPERATURE;
import static net.dries007.tfc.api.capability.heat.CapabilityItemHeat.MIN_TEMPERATURE;
import static tfctech.objects.blocks.devices.BlockElectricForge.LIT;

@SuppressWarnings("WeakerAccess")
@ParametersAreNonnullByDefault
public class TEElectricForge extends TEInventory implements ITickable, ITileFields
{
    public static final int SLOT_INPUT_MIN = 0;
    public static final int SLOT_INPUT_MAX = 8;
    public static final int SLOT_EXTRA_MIN = 9;
    public static final int SLOT_EXTRA_MAX = 11;
    private HeatRecipe[] cachedRecipes = new HeatRecipe[9];
    private float targetTemperature = 0.0F;
    private MachineEnergyContainer energyContainer;
    private int litTime = 0; //visual only

    public TEElectricForge()
    {
        super(12);
        energyContainer = new MachineEnergyContainer(ModConfig.DEVICES.electricForgeEnergyCapacity, ModConfig.DEVICES.electricForgeEnergyCapacity, 0);
        for (int i = 0; i < cachedRecipes.length; i++)
        {
            cachedRecipes[i] = null;
        }
    }

    public void addTargetTemperature(int value)
    {
        targetTemperature += value;
        if (targetTemperature > (float) ModConfig.DEVICES.electricForgeMaxTemperature) targetTemperature = (float) ModConfig.DEVICES.electricForgeMaxTemperature;
        if (targetTemperature < MIN_TEMPERATURE) targetTemperature = MIN_TEMPERATURE;
    }

    @Override
    public void update()
    {
        if (world.isRemote) return;
        IBlockState state = world.getBlockState(pos);
        boolean isLit = state.getValue(LIT);
        int energyUsage = (int) ((float) ModConfig.DEVICES.electricForgeEnergyConsumption * targetTemperature / 100);
        if(energyUsage < 1)energyUsage = 1;
        for (int i = SLOT_INPUT_MIN; i <= SLOT_INPUT_MAX; i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            IItemHeat cap = stack.getCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null);
            float modifier = stack.getItem() instanceof IMetalItem ? ((IMetalItem) stack.getItem()).getSmeltAmount(stack) / 100.0F : 1.0F;
            if (cap != null)
            {
                // Update temperature of item
                float itemTemp = cap.getTemperature();
                int energy = (int) (energyUsage * modifier);
                if (targetTemperature > itemTemp && energyContainer.consumeEnergy(energy, false))
                {
                    float heatSpeed = (float)ModConfig.DEVICES.electricForgeSpeed * 15.0F;
                    float temp = cap.getTemperature() + heatSpeed * cap.getHeatCapacity() * (float) ConfigTFC.GENERAL.temperatureModifierGlobal;
                    cap.setTemperature(temp > targetTemperature ? targetTemperature : temp);
                    litTime = 15;
                    if (!isLit)
                    {
                        isLit = true;
                        state = state.withProperty(LIT, true);
                        world.setBlockState(pos, state, 2);
                    }
                }
                handleInputMelting(stack, i);
            }
        }
        if (--litTime <= 0)
        {
            litTime = 0;
            if (isLit)
            {
                state = state.withProperty(LIT, false);
                world.setBlockState(pos, state, 2);
            }
        }
    }

    @Override
    public void setAndUpdateSlots(int slot)
    {
        this.markDirty();
        updateCachedRecipes();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        targetTemperature = nbt.getFloat("targetTemperature");
        energyContainer.deserializeNBT(nbt.getCompoundTag("energyContainer"));
        super.readFromNBT(nbt);

        updateCachedRecipes();
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt.setFloat("targetTemperature", targetTemperature);
        nbt.setTag("energyContainer", energyContainer.serializeNBT());
        return super.writeToNBT(nbt);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack)
    {
        if (slot <= SLOT_INPUT_MAX)
        {
            return stack.hasCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null);
        }
        else
        {
            return stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null) && stack.hasCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null);
        }
    }

    public boolean isLit()
    {
        return litTime > 0;
    }

    @Override
    public int getFieldCount()
    {
        return 2;
    }

    public int getEnergyCapacity()
    {
        return energyContainer.getMaxEnergyStored();
    }

    @Override
    public void setField(int index, int value)
    {
        if (index == 0)
        {
            this.targetTemperature = (float) value;
        }
        else if (index == 1)
        {
            this.energyContainer.setEnergy(value);
        }
        else
        {
            TFCTech.getLog().warn("Invalid field ID {} in TEElectricForge#setField", index);
        }
    }

    @Override
    public int getField(int index)
    {
        if (index == 0)
        {
            return (int) this.targetTemperature;
        }
        else if (index == 1)
        {
            return this.energyContainer.getEnergyStored();
        }
        else
        {
            TFCTech.getLog().warn("Invalid field ID {} in TEElectricForge#setField", index);
            return 0;
        }
    }

    private void handleInputMelting(ItemStack stack, int index)
    {
        HeatRecipe recipe = cachedRecipes[index];
        IItemHeat cap = stack.getCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null);

        if (recipe != null && cap != null && recipe.isValidTemperature(cap.getTemperature()))
        {
            // Handle possible metal output
            FluidStack fluidStack = recipe.getOutputFluid(stack);
            float itemTemperature = cap.getTemperature();
            if (fluidStack != null)
            {
                // Loop through all input slots
                for (int i = SLOT_EXTRA_MIN; i <= SLOT_EXTRA_MAX; i++)
                {
                    // While the fluid is still waiting
                    if (fluidStack.amount <= 0)
                    {
                        break;
                    }
                    // Try an output slot
                    ItemStack output = inventory.getStackInSlot(i);
                    // Fill the fluid
                    IFluidHandler fluidHandler = output.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                    if (fluidHandler != null)
                    {
                        int amountFilled = fluidHandler.fill(fluidStack.copy(), true);
                        if (amountFilled > 0)
                        {
                            fluidStack.amount -= amountFilled;

                            // If the fluid was filled, make sure to make it the same temperature
                            IItemHeat heatHandler = output.getCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null);
                            if (heatHandler != null)
                            {
                                heatHandler.setTemperature(itemTemperature);
                            }
                        }
                    }
                }
            }

            // Handle possible item output
            inventory.setStackInSlot(index, recipe.getOutputStack(stack));
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing)
    {
        return (capability == CapabilityEnergy.ENERGY && facing == EnumFacing.UP) || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
        return capability == CapabilityEnergy.ENERGY && facing == EnumFacing.UP ? (T) this.energyContainer : super.getCapability(capability, facing);
    }

    private void updateCachedRecipes()
    {
        for (int i = SLOT_INPUT_MIN; i <= SLOT_INPUT_MAX; ++i)
        {
            this.cachedRecipes[i] = null;
            ItemStack inputStack = this.inventory.getStackInSlot(i);
            if (!inputStack.isEmpty())
            {
                this.cachedRecipes[i] = HeatRecipe.get(inputStack);
            }
        }
    }
}