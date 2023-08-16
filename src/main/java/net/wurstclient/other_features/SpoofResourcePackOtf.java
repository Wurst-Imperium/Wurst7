package net.wurstclient.other_features;

import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;

public final class SpoofResourcePackOtf extends OtherFeature {

    private final CheckboxSetting disableResourcePacks =
            new CheckboxSetting("Disable downloaded resource packs", false);

    public SpoofResourcePackOtf()
    {
        super("SpoofResourcePack",
                "Skips downloading and success confirmation of resource packs for some servers");
        addSetting(disableResourcePacks);
    }

    @Override
    public boolean isEnabled()
    {
        return disableResourcePacks.isChecked();
    }

    @Override
    public String getPrimaryAction()
    {
        return isEnabled() ? "Re-enable downloading" : "Disable downloading";
    }

    @Override
    public void doPrimaryAction()
    {
        disableResourcePacks.setChecked(!disableResourcePacks.isChecked());
    }

    // See ClientPlayNetworkHandlerMixin::onOnResourcePackSend

}
