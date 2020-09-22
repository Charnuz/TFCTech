package tfctech;

import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import tfctech.client.TechGuiHandler;
import tfctech.network.PacketFridgeUpdate;
import tfctech.network.PacketLatexUpdate;
import tfctech.network.PacketTileEntityUpdate;
import tfctech.registry.TechFoodTraits;

@SuppressWarnings("WeakerAccess")
@Mod(modid = TFCTech.MODID, name = TFCTech.NAME, version = TFCTech.VERSION, dependencies = TFCTech.DEPENDENCIES)
public class TFCTech
{
    public static final String MODID = "tfctech";
    public static final String NAME = "TFCTech Unofficial";
    public static final String VERSION = "@VERSION@";
    public static final String DEPENDENCIES = "required-after:tfc@[1.0.0,);after:ic2;after:gregtech";
    private static final boolean signedBuild = true;
    @SuppressWarnings("FieldMayBeFinal")
    @Mod.Instance
    private static TFCTech instance = null;
    private static Logger logger;

    public static SimpleNetworkWrapper getNetwork()
    {
        return instance.network;
    }

    public static Logger getLog()
    {
        return logger;
    }

    public static TFCTech getInstance()
    {
        return instance;
    }

    private SimpleNetworkWrapper network;

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", "tfctech.compat.waila.TOPPlugin");
        TechFoodTraits.init();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new TechGuiHandler());
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        int id = 0;
        network.registerMessage(new PacketLatexUpdate.Handler(), PacketLatexUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketTileEntityUpdate.Handler(), PacketTileEntityUpdate.class, ++id, Side.CLIENT);
        network.registerMessage(new PacketFridgeUpdate.Handler(), PacketFridgeUpdate.class, ++id, Side.CLIENT);
        if (!signedBuild)
        {
            logger.error("INVALID FINGERPRINT DETECTED! This means this jar file has been compromised and are not supported.");
        }
    }
}
