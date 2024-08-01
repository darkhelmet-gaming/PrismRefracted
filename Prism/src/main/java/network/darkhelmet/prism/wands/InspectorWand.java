package network.darkhelmet.prism.wands;

import network.darkhelmet.prism.Il8nHelper;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionMessage;
import network.darkhelmet.prism.actionlibs.QueryParameters;
import network.darkhelmet.prism.actionlibs.QueryResult;
import network.darkhelmet.prism.api.actions.MatchRule;
import network.darkhelmet.prism.api.commands.Flag;
import network.darkhelmet.prism.text.ReplaceableTextComponent;
import network.darkhelmet.prism.utils.MiscUtils;
import network.darkhelmet.prism.utils.block.Utilities;
import net.kyori.adventure.text.format.NamedTextColor;
import network.darkhelmet.prism.api.actions.Handler;
import network.darkhelmet.prism.utils.folia.PrismScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

public class InspectorWand extends QueryWandBase {

    /**
     * Constructor.
     * @param plugin Prism
     */
    public InspectorWand(Prism plugin) {
        super(plugin);
    }


    @Override
    public void playerRightClick(Player player, Entity entity) {
    }

    @Override
    public void playerRightClick(Player player, Location loc) {
        showLocationHistory(player, loc);
    }

    @Override
    public void playerLeftClick(Player player, Location loc) {
        showLocationHistory(player, loc);
    }

    /**
     * Show the Location History for a player.
     *
     * @param player Player
     * @param loc    Location
     */
    private void showLocationHistory(final Player player, final Location loc) {

        final Block block = loc.getBlock();
        final Block sibling = Utilities.getSiblingForDoubleLengthBlock(block);
        BlockData blockData = block.getBlockData();
        Material type = blockData.getMaterial();
        PrismScheduler.runTaskAsynchronously(() -> {

            // Build params
            QueryParameters params;

            try {
                params = parameters.clone();
            } catch (final CloneNotSupportedException ex) {
                params = new QueryParameters();
                Prism.messenger.sendMessage(player, Prism.messenger
                        .playerError("Error retrieving parameters. Checking with default parameters."));
            }
            params.setWorld(player.getWorld().getName());
            params.setSpecificBlockLocation(loc);

            // Do we need a second location? (For beds, doors, etc)
            if (sibling != null) {
                params.addSpecificBlockLocation(sibling.getLocation());
            }

            // Ignoring any actions via config?
            if (params.getActionTypes().size() == 0) {
                @SuppressWarnings("unchecked") final Collection<String> ignoreActions =
                        (ArrayList<String>) plugin.getConfig().getList("prism.wands.inspect.ignore-actions");
                if (ignoreActions != null && !ignoreActions.isEmpty()) {
                    for (final String ignore : ignoreActions) {
                        params.addActionType(ignore, MatchRule.EXCLUDE);
                    }
                }
            }
            final QueryResult results = getResult(params, player);
            if (!results.getActionResults().isEmpty()) {

                final String blockname = Prism.getItems().getAlias(type, blockData);
                Prism.messenger.sendMessage(player,
                        Prism.messenger.playerHeaderMsg(ReplaceableTextComponent.builder("inspector-wand-header")
                                .replace("<blockname>",blockname)
                                .replace("<x>",loc.getBlockX())
                                .replace("<y>",loc.getBlockY())
                                .replace("<z>",loc.getBlockZ())
                                .build().colorIfAbsent(NamedTextColor.GOLD)));
                Prism.messenger.sendMessage(player,
                        Prism.messenger.playerHeaderMsg(Il8nHelper.formatMessage("lookup-header-message",
                                results.getTotalResults(), 1, results.getTotalPages())));
                int resultCount = results.getIndexOfFirstResult();
                for (final Handler a : results.getPaginatedActionResults()) {
                    final ActionMessage am = new ActionMessage(a);
                    if (parameters.hasFlag(Flag.EXTENDED)
                            || plugin.getConfig().getBoolean("prism.messenger.always-show-extended")) {
                        am.showExtended();
                    }
                    am.setResultIndex(resultCount);
                    MiscUtils.sendClickableTpRecord(am, player);
                    resultCount++;
                }
                MiscUtils.sendPageButtons(results, player);
            } else {
                final String space_name = (type.equals(Material.AIR) ? "space"
                        : type.toString().replaceAll("_", " ").toLowerCase()
                        + (type.toString().endsWith("BLOCK") ? "" : " block"));
                Prism.messenger.sendMessage(player,
                        Prism.messenger.playerError("No history for this " + space_name + " found."));
            }
        });
    }

}
