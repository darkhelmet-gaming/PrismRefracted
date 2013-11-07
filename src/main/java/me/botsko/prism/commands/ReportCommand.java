package me.botsko.prism.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.botsko.elixr.TypeUtils;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;

public class ReportCommand implements SubHandler {
	
	/**
	 * 
	 */
	private Prism plugin;
	
	
	/**
	 * 
	 * @param plugin
	 * @return 
	 */
	public ReportCommand(Prism plugin) {
		this.plugin = plugin;
	}
	
	
	/**
	 * Handle the command
	 */
	public void handle(CallInfo call) {
		
		if(call.getArgs().length < 2){
			call.getSender().sendMessage( Prism.messenger.playerError( "Please specify a report. Use /prism ? for help." ) );
			return;
		}
		
		// /prism report queue
		if(call.getArg(1).equals("queue")){
			queueReport( call.getSender() );
		}
		
		// /prism report queue
		if(call.getArg(1).equals("sum")){
			
			if( call.getArgs().length < 3 ){
				call.getSender().sendMessage( Prism.messenger.playerError( "Please specify a 'sum' report. Use /prism ? for help." ) );
				return;
			}
			
			if( call.getArgs().length < 4 ){
				call.getSender().sendMessage( Prism.messenger.playerError( "Please provide a player name. Use /prism ? for help." ) );
				return;
			}
			
			if( call.getArg(2).equals("blocks") ){
				blockSumReports( call.getSender(), call.getArg(3) );
			}
			
		}
	}
	
	
	/**
	 * 
	 * @param sender
	 */
	protected void queueReport( CommandSender sender ){
		
		sender.sendMessage( Prism.messenger.playerHeaderMsg( "Current Stats") );
		
		sender.sendMessage( Prism.messenger.playerMsg( "Actions in save queue: " + ChatColor.WHITE + Prism.actionsRecorder.getQueueSize() ) );
		
		ConcurrentSkipListMap<Long,Integer> runs = plugin.queueStats.getRecentRunCounts();
		if(runs.size() > 0){
			sender.sendMessage( Prism.messenger.playerHeaderMsg( "Recent queue save stats:" ) );
			for (Entry<Long, Integer> entry : runs.entrySet()){
				String time = new SimpleDateFormat("HH:mm:ss").format( entry.getKey());
			    sender.sendMessage( Prism.messenger.playerMsg( ChatColor.GRAY + time + " " + ChatColor.WHITE + entry.getValue() ) );
			}
		}
	}
	
	
	/**
	 * 
	 * @param sender
	 */
	protected void blockSumReports( final CommandSender sender, final String playerName ){

		final String sql = "SELECT block_id, SUM(placed) AS placed, SUM(broken) AS broken "
				+ "FROM (("
					+ "SELECT block_id, COUNT(id) AS placed, 0 AS broken "
					+ "FROM prism_data d "
					+ "INNER JOIN prism_players p ON p.player_id = d.player_id "
					+ "INNER JOIN prism_actions a ON a.action_id = d.action_id "
					+ "WHERE player = ? "
					+ "AND action = 'block-place' "
					+ "GROUP BY block_id) "
				+ "UNION ( "
					+ "SELECT block_id, 0 AS placed, count(id) AS broken "
					+ "FROM prism_data d "
					+ "INNER JOIN prism_players p ON p.player_id = d.player_id "
					+ "INNER JOIN prism_actions a ON a.action_id = d.action_id "
					+ "WHERE player = ? "
					+ "AND action = 'block-break' "
					+ "GROUP BY block_id)) "
				+ "AS PR_A "
				+ "GROUP BY block_id ORDER BY (SUM(placed) + SUM(broken)) DESC;";
		
		final int colTextLen = 20;
		final int colIntLen = 12;
		
		/**
		 * Run the lookup itself in an async task so the lookup query isn't done on the main thread
		 */
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){
			public void run(){
				
				sender.sendMessage( Prism.messenger.playerSubduedHeaderMsg("Crafting block change report for " + ChatColor.DARK_AQUA + playerName + "...") );

				Connection conn = null;
				PreparedStatement s = null;
				ResultSet rs = null;
				try {

					conn = Prism.dbc();
		    		s = conn.prepareStatement(sql);
		    		s.setString(1, playerName);
		    		s.setString(2, playerName);
		    		s.executeQuery();
		    		rs = s.getResultSet();
		    		
		    		sender.sendMessage( Prism.messenger.playerHeaderMsg("Total block changes for " + ChatColor.DARK_AQUA + playerName) );
					sender.sendMessage( Prism.messenger.playerMsg( ChatColor.GRAY + TypeUtils.padStringRight( "Block", colTextLen ) + TypeUtils.padStringRight( "Placed", colIntLen ) + TypeUtils.padStringRight( "Broken", colIntLen ) ) );

		    		while(rs.next()){
		    			
		    			String alias = plugin.getItems().getAlias(rs.getInt(1), 0);
	
		    			int placed = rs.getInt(2);
		    			int broken = rs.getInt(3);
		    			
		    			String colAlias = TypeUtils.padStringRight( alias, colTextLen );
		    			String colPlaced = TypeUtils.padStringRight( ""+placed, colIntLen );
		    			String colBroken = TypeUtils.padStringRight( ""+broken, colIntLen );
		    			
		    			sender.sendMessage( Prism.messenger.playerMsg( ChatColor.DARK_AQUA + colAlias + ChatColor.GREEN + colPlaced + " " + ChatColor.RED + colBroken ) );
		    			
		    		}
		        } catch (SQLException e) {
		        	//
		        	e.printStackTrace();
		        } finally {
		        	if(rs != null) try { rs.close(); } catch (SQLException e) {}
		        	if(s != null) try { s.close(); } catch (SQLException e) {}
		        	if(conn != null) try { conn.close(); } catch (SQLException e) {}
		        }
			}
		});
	}
}