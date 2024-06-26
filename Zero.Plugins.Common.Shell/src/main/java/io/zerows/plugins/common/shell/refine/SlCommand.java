package io.zerows.plugins.common.shell.refine;

import io.vertx.core.json.JsonArray;
import io.vertx.up.util.Ut;
import io.zerows.plugins.common.shell.ConsoleCommander;
import io.zerows.plugins.common.shell.atom.CommandAtom;
import io.zerows.plugins.common.shell.commander.BackCommander;
import io.zerows.plugins.common.shell.commander.HelpCommander;
import io.zerows.plugins.common.shell.commander.QuitCommander;
import io.zerows.plugins.common.shell.eon.EmCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
class SlCommand {
    private static final List<CommandAtom> commands = new ArrayList<>();
    private static final ConcurrentMap<String, Class<?>> COMMAND_PLUGINS = new ConcurrentHashMap<String, Class<?>>() {
        {
            this.put("h", HelpCommander.class);
            this.put("q", QuitCommander.class);
            this.put("b", BackCommander.class);
        }
    };

    public static List<CommandAtom> commands() {
        if (commands.isEmpty()) {
            /*
             * Default Commands
             */
            final List<CommandAtom> defaults = mountDefault("h", "q");
            commands.addAll(defaults);

            /*
             * Mount default plugin
             */
            final JsonArray commandJson = SlConfig.commands();
            final List<CommandAtom> commandsList = Ut.fromJson(commandJson, CommandAtom.class);
            commandsList.stream().filter(command -> COMMAND_PLUGINS.containsKey(command.getSimple())).forEach(item ->
                Sl.failWarn("The command will be ignored: name = {0}, description: {1}", item.getName(), item.getDescription()));
            final List<CommandAtom> filtered = commandsList.stream()
                .filter(item -> !COMMAND_PLUGINS.containsKey(item.getSimple()))
                .collect(Collectors.toList());
            commands.addAll(mountPlugin(filtered));
        }
        return commands;
    }

    public static List<CommandAtom> commands(final List<CommandAtom> commands) {
        /*
         * Default Commands
         */
        final List<CommandAtom> source = Objects.isNull(commands) ? new ArrayList<>() : commands;
        source.stream().filter(command -> COMMAND_PLUGINS.containsKey(command.getSimple())).forEach(item ->
            Sl.failWarn("The command will be ignored: name = {0}, description: {1}", item.getName(), item.getDescription()));
        final List<CommandAtom> normalized = source.stream()
            .filter(command -> !COMMAND_PLUGINS.containsKey(command.getSimple()))
            .collect(Collectors.toList());
        /*
         * Default Commands
         */
        final List<CommandAtom> defaults = mountDefault("h", "b");
        normalized.addAll(defaults);

        /*
         * Mount default plugin
         */
        mountPlugin(normalized);

        return normalized;
    }

    private static List<CommandAtom> mountDefault(final String... includes) {
        final JsonArray commandsDefault = SlConfig.commandsDefault();
        final List<CommandAtom> commandsDefaultList = Ut.fromJson(commandsDefault, CommandAtom.class);
        /* Set contains */
        final Set<String> includeSet = new HashSet<>(Arrays.asList(includes));
        return commandsDefaultList.stream().filter(command -> includeSet.contains(command.getSimple()))
            .peek(command -> {
                command.setArgs(false);
                command.setType(EmCommand.Type.DEFAULT);
                command.setPlugin(COMMAND_PLUGINS.get(command.getSimple()));
            }).collect(Collectors.toList());
    }

    private static List<CommandAtom> mountPlugin(final List<CommandAtom> commands) {
        commands.stream().filter(item -> EmCommand.Type.SYSTEM == item.getType()).forEach(command -> {
            command.setArgs(false);
            command.setPlugin(ConsoleCommander.class);
        });
        return commands;
    }
}
