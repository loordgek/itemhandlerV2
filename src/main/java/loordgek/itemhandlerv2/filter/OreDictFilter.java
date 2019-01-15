package loordgek.itemhandlerv2.filter;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.ArrayUtils;

public class OreDictFilter implements IStackFilter {
    private final String oreName;

    public OreDictFilter(String oreName) {
        this.oreName = oreName;
    }

    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && ArrayUtils.contains(OreDictionary.getOreIDs(stack), OreDictionary.getOreID(oreName));
    }

    @Override
    public NonNullList<ItemStack> getExamples() {
        return OreDictionary.getOres(oreName);
    }
}
