package dev.zelevar;

import java.util.List;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/** Универсальная команда: /<команда> add|remove|list <игрок> для любой scoreboard-команды. */
public final class TeamCommand implements TabExecutor {
    private final Supplier<Team> teamSupplier;
    private final String title;

    public TeamCommand(Supplier<Team> teamSupplier, String title) {
        this.teamSupplier = teamSupplier;
        this.title = title;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        Team team = teamSupplier.get();
        if (a.length == 0) {
            s.sendMessage("Использование: /" + label + " <add|remove|list> [игрок]");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> s.sendMessage("Команда «" + title + "»: " + (team.getEntries().isEmpty()
                ? "никого" : String.join(", ", team.getEntries())));
            case "add", "remove" -> {
                if (a.length < 2) { s.sendMessage("Укажите игрока."); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { s.sendMessage("Игрок не в сети."); return true; }
                if (a[0].equalsIgnoreCase("add")) {
                    team.addEntry(t.getName());
                    s.sendMessage(t.getName() + " добавлен в команду «" + title + "».");
                } else {
                    team.removeEntry(t.getName());
                    s.sendMessage(t.getName() + " убран из команды «" + title + "».");
                }
            }
            default -> s.sendMessage("Неизвестная подкоманда.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return List.of("add", "remove", "list");
        if (a.length == 2 && !a[0].equalsIgnoreCase("list"))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}
