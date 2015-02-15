package adventureArena;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.event.block.SignChangeEvent;

public class AA_SignCommand {

	private final Map<String, String> parameterMap;
	private final String command;

	private AA_SignCommand(final String command, final Map<String, String> parameterMap) {
		this.command = command;
		this.parameterMap = parameterMap;
	}

	public static AA_SignCommand createFrom(final String[] lines, final SignChangeEvent event) {
		String command = null;
		Map<String, String> parameterMap = null;

		for (int i = 0; i<lines.length; i++) {
			String line = lines[i];
			if (command != null) {
				String[] keyVal = line.split(":", 2);
				if (keyVal.length == 2) {
					parameterMap.put(keyVal[0], keyVal[1]);
				}
			}
			if (line.startsWith("[") && line.endsWith("]")) {
				command = line.substring(1, line.length()-1);
				if (event != null) {
					event.setLine(i, "[" + ChatColor.BLUE + command + ChatColor.BLACK + "]");
				}
				parameterMap = new HashMap<String, String>();
			}
		}
		return command == null ? null : new AA_SignCommand(command, parameterMap);
	}

	public void execute() {
		switch (command) {

		}

	}

}
