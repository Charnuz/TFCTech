package tfctech.objects.fluids;

import javax.annotation.Nonnull;

import com.google.common.collect.HashBiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import net.dries007.tfc.api.util.TFCConstants;
import net.dries007.tfc.objects.fluids.properties.FluidWrapper;

public final class TechFluids
{
    private static final ResourceLocation LAVA_STILL = new ResourceLocation(TFCConstants.MOD_ID, "blocks/lava_still");
    private static final ResourceLocation LAVA_FLOW = new ResourceLocation(TFCConstants.MOD_ID, "blocks/lava_flow");
    private static final HashBiMap<Fluid, FluidWrapper> WRAPPERS = HashBiMap.create();

    public static final float GLASS_MELT_TEMPERATURE = 800f;

    public static FluidWrapper LATEX;
    public static FluidWrapper GLASS;

    public static void registerFluids()
    {
        LATEX = registerFluid(new Fluid("latex", LAVA_STILL, LAVA_FLOW, 0xFFF8F8F8));
        GLASS = registerFluid(new Fluid("glass", LAVA_STILL, LAVA_FLOW, 0xFFC0F5FE));
    }

    private static FluidWrapper registerFluid(@Nonnull Fluid newFluid)
    {
        boolean isDefault = FluidRegistry.registerFluid(newFluid);
        if (!isDefault)
        {
            // Fluid was already registered with this name, default to that fluid
            newFluid = FluidRegistry.getFluid(newFluid.getName());
        }
        FluidRegistry.addBucketForFluid(newFluid);
        FluidWrapper properties = new FluidWrapper(newFluid, isDefault);
        WRAPPERS.put(newFluid, properties);
        return properties;
    }
}
