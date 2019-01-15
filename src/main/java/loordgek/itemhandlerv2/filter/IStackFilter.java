package loordgek.itemhandlerv2.filter;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.function.Predicate;

@FunctionalInterface
public interface IStackFilter extends Predicate<ItemStack> {
    NonNullList<ItemStack> emptyNNList = NonNullList.withSize(0, ItemStack.EMPTY);

    @Override
    boolean test(ItemStack stack);

    default NonNullList<ItemStack> getExamples() {
        return emptyNNList;
    }
}
